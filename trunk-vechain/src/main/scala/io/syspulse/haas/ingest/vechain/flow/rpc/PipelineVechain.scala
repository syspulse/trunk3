package io.syspulse.haas.ingest.vechain.flow.rpc

import java.util.concurrent.atomic.AtomicLong
import io.syspulse.skel.ingest.flow.Flows

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

import akka.util.ByteString
import akka.http.scaladsl.model.{HttpRequest,HttpMethods,HttpEntity,ContentTypes}
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Flow

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util
import io.syspulse.skel.config._

import io.syspulse.skel.ingest._
import io.syspulse.skel.ingest.store._
import io.syspulse.skel.ingest.flow.Pipeline

import spray.json._
import DefaultJsonProtocol._
import io.syspulse.skel.serde.Parq._
import com.github.mjakubowski84.parquet4s.{ParquetRecordEncoder,ParquetSchemaResolver}

import java.util.concurrent.TimeUnit

import io.syspulse.haas.ingest.vechain.flow.rpc._
import io.syspulse.haas.ingest.vechain.flow.rpc.VechainRpcJson._

import io.syspulse.haas.ingest.vechain.VechainURI
import io.syspulse.haas.ingest.PipelineIngest
import io.syspulse.haas.ingest.vechain

import io.syspulse.haas.ingest.Config

import akka.actor.typed.ActorSystem
import akka.stream.RestartSettings
import scala.util.control.NoStackTrace
import requests.Response
import akka.stream.scaladsl.Sink

import io.syspulse.haas.ingest.CursorBlock

class RetryException(msg: String) extends RuntimeException(msg) with NoStackTrace

case class BlockId(index:Long,hash:String)

// ATTENTION !!!
// throttle is overriden in Config to support batchable retries !
abstract class PipelineVechain[T,O <: skel.Ingestable,E <: skel.Ingestable](config:Config)
                                                                       (implicit fmt:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E])
  extends PipelineIngest[T,O,E](config.copy(throttle = 0L))(fmt,parqEncoders,parsResolver) with VechainDecoder[E] {

  override val retrySettings:Option[RestartSettings] = Some(RestartSettings(
    minBackoff = FiniteDuration(1000,TimeUnit.MILLISECONDS),
    maxBackoff = FiniteDuration(1000,TimeUnit.MILLISECONDS),
    randomFactor = 0.2
  ))

  import VechainRpcJson._

  val cursor = new CursorBlock("BLOCK-vechain")  
    
  override def source(feed:String) = {
    feed.split("://").toList match {
      case "http" :: _ | "https" :: _ | "vechain" :: _ => 

        val rpcUri = VechainURI(feed,apiToken = config.apiToken)
        val uri = rpcUri.uri
        log.info(s"uri=${uri}")
        
        val blockStr = config.block.split("://").toList match {
          case "file" :: file :: Nil => cursor.read(file)
          case "file" :: Nil => cursor.read()
          case _ => config.block
        }

        val blockStart:Long = blockStr.strip match {
          case "latest" =>
            val rsp = requests.get(s"${uri}/blocks/best",
              headers = Seq(("Content-Type","application/json")),
            )
            
            if(rsp.statusCode != 200) {
              log.error(s"failed to get latest block: ${rsp}")
              0
            } else {
              val r = ujson.read(rsp.text())
              r.obj("number").num.toLong
            }
          case hex if hex.startsWith("0x") =>
            val index = java.lang.Long.parseLong(hex.drop(2),16).toLong
            index
          case dec =>
            val index = dec.toLong
            index
        }
        
        val blockEnd = config.blockEnd match {
          case "" => Int.MaxValue
          case "latest" => blockStart
          case hex if hex.startsWith("0x") =>
            java.lang.Long.parseLong(hex,16).toLong
          case _ @ dec =>
            dec.toLong
        }

        cursor.init(blockStart - config.blockLag, blockEnd)
                   
        log.info(s"cursor: ${cursor}")        

        val sourceTick = Source.tick(
          FiniteDuration(10,TimeUnit.MILLISECONDS), 
          //FiniteDuration(config.ingestCron.toLong,TimeUnit.SECONDS),
          FiniteDuration(config.throttle,TimeUnit.MILLISECONDS),
          s"ingest-vechain-${feed}"
        )
                
        // ------- Flow ------------------------------------------------------------------------------------
        sourceTick
          .map(h => {
            log.debug(s"Cron --> ${h}")

            // request latest block to know where we are from current
            val rsp = requests.get(s"${uri}/blocks/best?expanded=false",
              headers = Map("content-type" -> "application/json")
            )
            //log.info(s"rsp=${rsp.statusCode}: ${rsp.text()}")
            rsp.statusCode match {
              case 200 => //
              case _ => 
                // retry
                log.error(s"RPC error: ${rsp.statusCode}: ${rsp.text()}")
                throw new RetryException("")
            }
            
            val r = ujson.read(rsp.text())
            val lastBlock = r.obj("number").num.toLong
            
            log.info(s"last=${lastBlock}, current=${cursor.get()}, lag=${config.blockLag}")
            lastBlock - config.blockLag
          })
          .mapConcat(lastBlock => {
            // ATTENTION:
            // lag and reorg are not compatible !            
            if(config.blockReorg == 0 || cursor.get() < (lastBlock - config.blockReorg))              
              // normal fast operation or reorg before the tip
              cursor.get() to lastBlock
            else
              // reorg operation on the tip
              (cursor.get() - config.blockReorg) to lastBlock
          })          
          .groupedWithin(config.blockBatch,FiniteDuration(1,TimeUnit.MILLISECONDS)) // batch limiter 
          .map(blocks => blocks.filter(_ <= blockEnd))
          .takeWhile(blocks => // limit flow by the specified end block
            blocks.filter(_ <= blockEnd).size > 0
          )
          .mapConcat(blocks => {
            log.info(s"--> ${blocks}")

            blocks
          })
          .map( block => {
            val rsp = requests.get(
              s"${uri}/blocks/${block}", 
              headers = Map("content-type" -> "application/json")
            )
            log.info(s"rsp=${rsp.statusCode}")
            
            rsp.statusCode match {
              case 200 => //
                
              case _ => 
                // retry
                log.error(s"RPC error: ${rsp.statusCode}: ${rsp.text()}")
                throw new RetryException("")
            }

            rsp.text()
          })
          //.filter(reorgFlow)
          .map(b => ByteString(b))
      
      case _ => super.source(feed)
    }
  }

  def decodeSingle(rsp:String):Seq[String] = Seq(rsp)
  def decodeBatch(rsp:String):Seq[String] = {
    // ATTENTION !!!
    // very inefficient, optimize with web3-proxy approach 
    val jsonBatch = ujson.read(rsp)
    jsonBatch.arr.map(a => a.toString()).toSeq
  }
}
