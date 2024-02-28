package io.syspulse.haas.ingest.icp.flow.rosetta

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

import io.syspulse.haas.ingest.icp.flow.rosetta._
import io.syspulse.haas.ingest.icp.flow.rosetta.IcpRpcJson._

import io.syspulse.haas.ingest.icp.IcpURI
import io.syspulse.haas.ingest.PipelineIngest
import io.syspulse.haas.ingest.icp

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
abstract class PipelineIcp[T,O <: skel.Ingestable,E <: skel.Ingestable](config:Config)
                                                                       (implicit fmt:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E])
  extends PipelineIngest[T,O,E](config.copy(throttle = 0L))(fmt,parqEncoders,parsResolver) with IcpDecoder[E] {

  override val retrySettings:Option[RestartSettings] = Some(RestartSettings(
    minBackoff = FiniteDuration(1000,TimeUnit.MILLISECONDS),
    maxBackoff = FiniteDuration(1000,TimeUnit.MILLISECONDS),
    randomFactor = 0.2
  ))

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
                  Some(
                    requests.post(uri.uri + "/network/status",
                      headers = Seq(("Content-Type","application/json")),
                      data = s"""{"network_identifier":{"blockchain":"${uri.blockchain}","network":"${uri.network}"}}""")
                    )
                } catch {
                  case e:Exception => 
                    log.error(s"request latest block failed -> ${uri.uri}",e)
                    Thread.sleep(config.throttle)
                    None
                }              
              } 
              rsp.get
            }

            // {
            //   "current_block_identifier": {
            //     "index": 7323684,
            //     "hash": "09fdf7f3501b1b311e6b13c8228cca74115abb3f6fabf1715c346da8a5128af1"
            //   },
            //   "current_block_timestamp": 1701443858809,
            //   "genesis_block_identifier": {
            //     "index": 0,
            //     "hash": "ffca0ecf5e837541c7ee5be431e433ad8e972a7f371e86fbe4f8ad646c7cbcea"
            //   },
            //   "sync_status": {
            //     "current_index": 7323684,
            //     "target_index": 7323684
            //   },
            //   "peers": []
            // }

            if(rsp.statusCode != 200) {
              log.error(s"failed to get latest block: ${rsp}")
              0
            } else {
              val r = ujson.read(rsp.text())
              val index = r.obj("current_block_identifier").obj("index").num.toLong
              val hash = r.obj("current_block_identifier").obj("hash").str
              index
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
              requests.post(uri.uri + "/network/status",
              headers = Seq(("Content-Type","application/json")),
              data = s"""{"network_identifier":{"blockchain":"${uri.blockchain}","network":"${uri.network}"}}"""
            )} catch {
              case e:Exception =>
                log.error(s"failed to get latest block: ${e}")
                throw e
            } 

            //log.info(s"rsp=${rsp.statusCode}: ${rsp.text()}")
            val lastBlock = if(rsp.statusCode != 200) {
              log.error(s"failed to get latest block: ${rsp}")
              0
            } else {
              val r = ujson.read(rsp.text())
              val index = r.obj("current_block_identifier").obj("index").num.toLong
              val hash = r.obj("current_block_identifier").obj("hash").str
              index
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
          .takeWhile(blocks =>  // limit flow by the specified end block
            blocks.filter(_ <= blockEnd).size > 0
          )
          .mapConcat(blocks => {
            log.info(s"--> ${blocks}")
            blocks
          })
          .map( block => {
            val json = 
              s"""{"network_identifier":{"blockchain":"${uri.blockchain}","network":"${uri.network}"},"block_identifier": {"index": ${block}}}"""

            try {
              val rsp = requests.post(uri.uri + "/block", data = json,headers = Map("content-type" -> "application/json"))              
              
              rsp.statusCode match {
                case 200 => //
                case _ => 
                  // retry
                  log.error(s"RPC error: ${rsp.statusCode}: ${rsp.text()}")
                  throw new RetryException("")
              }
              
              rsp.text()              

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
