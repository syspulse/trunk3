package io.syspulse.haas.ingest.icp.flow.ledger

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

import com.github.mjakubowski84.parquet4s.{ParquetRecordEncoder,ParquetSchemaResolver}

import java.util.concurrent.TimeUnit

import io.syspulse.haas.ingest.icp.flow.ledger._
import io.syspulse.haas.ingest.icp.flow.ledger.IcpRpcJson._

import io.syspulse.haas.ingest.icp.IcpURI
import io.syspulse.haas.ingest.PipelineIngest
import io.syspulse.haas.ingest.icp

import io.syspulse.haas.ingest.Config

import akka.actor.typed.ActorSystem
import akka.stream.RestartSettings
import scala.util.control.NoStackTrace
import requests.Response
import akka.stream.scaladsl.Sink

import io.syspulse.haas.core.RetryException
import io.syspulse.haas.ingest.CursorBlock


// ATTENTION !!!
// throttle is overriden in Config to support batchable retries !
abstract class PipelineIcp[T,O <: skel.Ingestable,E <: skel.Ingestable](config:Config)
                                                                       (implicit fmt:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E])
  extends PipelineIngest[T,O,E](config.copy(throttle = 0L))(fmt,parqEncoders,parsResolver) with IcpDecoder[E] {

  import IcpRpcJson._

  val cursor = new CursorBlock("BLOCK-icp")(config)  
  implicit val uri = IcpURI(config.feed,config.apiToken)
    
  override def source(feed:String) = {
    feed.split("://").toList match {
      case "http" :: _ | "https" :: _ | "icp" :: _ => 

        // val rpcUri = IcpURI(feed)
        // val uri = rpcUri.uri
        // val blockchain = rpcUri.blockchain
        // val network = rpcUri.network        
        log.info(s"uri=${uri.uri}, blockchain=${uri.blockchain}/${uri.network}")
        
        val blockStr = 
          (config.block.split("://").toList match {
            // start from latest and save to file
            case "latest" :: file :: Nil => cursor.setFile(file).read(); "latest"
            case "last" :: file :: Nil => cursor.setFile(file).read(); "latest"
            case "file" :: file :: Nil => cursor.setFile(file).read()
            case "file" :: Nil => cursor.read()
            // start block and save to file (10://file.txt)
            case block :: file :: Nil => cursor.setFile(file).read(); block
            case _ => config.block
          })

        val blockStart:Long = blockStr.strip match {
          case "latest" =>
            
            val rsp = { 
              var rsp:Option[requests.Response] = None
              while(!rsp.isDefined)  {
                rsp = try {
                  Some(requests.get(uri.uri + "/transactions?limit=1",headers = Seq(("Content-Type","application/json"))))
                } catch {
                  case e:Exception => 
                    log.error(s"request latest block failed -> ${uri.uri}",e)
                    Thread.sleep(config.throttle)
                    None
                }              
              } 
              rsp.get
            }
            
            if(rsp.statusCode != 200) {
              log.error(s"failed to get latest block: ${rsp}")
              0
            } else {
              val r = ujson.read(rsp.text())
              val total = r.obj("total").num.toLong
              total - 1
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
          s"ingest-icp-${feed}"
        )
                
        // ------- Flow ------------------------------------------------------------------------------------
        sourceTick
          .map(h => {
            log.debug(s"Cron --> ${h}")

            // request latest block to know where we are from current
            val rsp = try {
              requests.get(uri.uri + "/transactions?limit=1",headers = Seq(("Content-Type","application/json")))
            } catch {
              case e:Exception =>
                log.error(s"failed to get latest block: ${e}")
                throw e
            } 

            log.debug(s"rsp=${rsp.statusCode}: ${rsp.text()}")
            
            val lastBlock = if(rsp.statusCode != 200) {
              log.error(s"failed to get latest block: ${rsp}")
              0
            } else {
              val r = ujson.read(rsp.text())
              val total = r.obj("total").num.toLong
              total - 1
            }
                        
            log.info(s"last=${lastBlock}, current=${cursor.get()}, lag=${config.blockLag}")
            lastBlock - config.blockLag
          })
          .mapConcat(lastBlock => {
            cursor.get() to lastBlock            
          })          
          // batch limiter with a small tiny throttle
          .groupedWithin(config.blockBatch,FiniteDuration( 1L ,TimeUnit.MILLISECONDS)) 
          .map(blocks => blocks.filter(_ <= blockEnd))
          .map(blocks => 
            // distinct and checking for current commit this is needed because of backpressure in groupedWithin when Sink is restarted (like Kafka reconnect)
            // when downstream backpressur is working, it generated for every Cron tick a new Range which produces
            // duplicates since commit is not changing. 
            // Example: 
            // PipelineRPC.scala:237] --> Vector(61181547, 61181548, 61181549, 61181547, 61181548)
            // PipelineRPC.scala:237] --> Vector(61181549, 61181550, 61181547, 61181548, 61181549)
            blocks
            .distinct
            .filter(b => 
              b <= blockEnd 
              && 
              b >= cursor.get() 
            )
          )
          .mapConcat(blocks => {
            log.info(s"--> ${blocks}")
            blocks
          })
          .map( block => {            
            try {
              val rsp = requests.get(uri.uri + s"/transactions?limit=1&block_height=${block}", 
                headers = Map("content-type" -> "application/json")
              )

              val body = rsp.text()
              
              rsp.statusCode match {
                case 200 =>
                  log.trace(s"body='${body}'")
                case _ => 
                  // retry
                  log.error(s"RPC error: ${rsp.statusCode}: ${body}")
                  throw new RetryException("")
              }
                            
              body

            } catch {
              case e:Exception => 
                log.error(s"failed to get block: ${block}",e)
                throw e
            }            
          })
          .log(s"${feed}")
          .throttle(1,FiniteDuration(config.blockThrottle,TimeUnit.MILLISECONDS)) // throttle fast range group 
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
