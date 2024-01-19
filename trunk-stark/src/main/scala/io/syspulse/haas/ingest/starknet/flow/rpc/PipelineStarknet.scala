package io.syspulse.haas.ingest.starknet.flow.rpc

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

import io.syspulse.haas.ingest.starknet.flow.rpc._
import io.syspulse.haas.ingest.starknet.flow.rpc.StarknetRpcJson._

import io.syspulse.haas.ingest.starknet.StarknetURI
import io.syspulse.haas.ingest.PipelineIngest
import io.syspulse.haas.ingest.starknet

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
abstract class PipelineStarknet[T,O <: skel.Ingestable,E <: skel.Ingestable](config:Config)
                                                                       (implicit fmt:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E])
  extends PipelineIngest[T,O,E](config.copy(throttle = 0L))(fmt,parqEncoders,parsResolver) with StarknetDecoder[E] {

  override val retrySettings:Option[RestartSettings] = Some(RestartSettings(
    minBackoff = FiniteDuration(1000,TimeUnit.MILLISECONDS),
    maxBackoff = FiniteDuration(1000,TimeUnit.MILLISECONDS),
    randomFactor = 0.2
  ))

  import StarknetRpcJson._

  val cursor = new CursorBlock("BLOCK-starknet")  
    
  override def source(feed:String) = {
    feed.split("://").toList match {
      case "http" :: _ | "https" :: _ | "starknet" :: _ => 

        val rpcUri = StarknetURI(feed,apiToken = config.apiToken)
        val uri = rpcUri.uri
        log.info(s"uri=${uri}")
        
        val blockStr = config.block.split("://").toList match {
          case "file" :: file :: Nil => cursor.read(file)
          case "file" :: Nil => cursor.read()
          case _ => config.block
        }

        val blockStart:Long = blockStr.strip match {
          case "latest" =>
            val rsp = requests.post(uri,
              headers = Seq(("Content-Type","application/json")),
              data = s"""{"jsonrpc":"2.0","method":"starknet_blockNumber","params":[],"id":1}"""
            )
            
            if(rsp.statusCode != 200) {
              log.error(s"failed to get latest block: ${rsp}")
              0
            } else {
              val r = ujson.read(rsp.text())
              r.obj("result").num.toLong
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
          s"ingest-starknet-${feed}"
        )
                
        // ------- Flow ------------------------------------------------------------------------------------
        sourceTick
          .map(h => {
            log.debug(s"Cron --> ${h}")

            // request latest block to know where we are from current
            val blockHex = "latest"
            val json = s"""{
                "jsonrpc":"2.0","method":"starknet_blockNumber",
                "params":[],
                "id": 0
              }""".trim.replaceAll("\\s+","")

            val rsp = requests.post(uri, data = json,headers = Map("content-type" -> "application/json"))
            //log.info(s"rsp=${rsp.statusCode}: ${rsp.text()}")
            rsp.statusCode match {
              case 200 => //
              case _ => 
                // retry
                log.error(s"RPC error: ${rsp.statusCode}: ${rsp.text()}")
                throw new RetryException("")
            }
            
            val r = ujson.read(rsp.text())
            val lastBlock = r.obj("result").num.toLong
            
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
            
            val blocksReq = blocks.map(block => {              
              s"""{
                  "jsonrpc":"2.0","method":"starknet_getBlockWithTxs",
                  "params":[{"block_number":${block}}],
                  "id":0
                }""".trim.replaceAll("\\s+","")
            })
                        
            val json = s"""[${blocksReq.mkString(",")}]"""
            val rsp = requests.post(uri, data = json,headers = Map("content-type" -> "application/json"))
            log.info(s"rsp=${rsp.statusCode}")
            
            rsp.statusCode match {
              case 200 => //
                
              case _ => 
                // retry
                log.error(s"RPC error: ${rsp.statusCode}: ${rsp.text()}")
                throw new RetryException("")
            }
            
            val batch = decodeBatch(rsp.text())            
            batch
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
