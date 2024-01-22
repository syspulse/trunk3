package io.syspulse.haas.ingest.eth.flow.rpc3

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

import io.syspulse.haas.ingest.eth.flow.rpc3._
import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson._

import io.syspulse.haas.ingest.eth.EthURI
import io.syspulse.haas.ingest.PipelineIngest
import io.syspulse.haas.ingest.eth

import io.syspulse.haas.ingest.Config

import akka.actor.typed.ActorSystem
import akka.stream.RestartSettings
import scala.util.control.NoStackTrace
import requests.Response
import akka.stream.scaladsl.Sink
import io.syspulse.haas.ingest.CursorBlock

class RetryException(msg: String) extends RuntimeException(msg) with NoStackTrace

// ATTENTION !!!
// throttle is overriden in Config to support batchable retries !
abstract class PipelineRPC[T,O <: skel.Ingestable,E <: skel.Ingestable](config:Config)
                                                                       (implicit fmt:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E])
  extends PipelineIngest[T,O,E](config.copy(throttle = 0L))(fmt,parqEncoders,parsResolver) with RPCDecoder[E] {

  override val retrySettings:Option[RestartSettings] = Some(RestartSettings(
    minBackoff = FiniteDuration(1000,TimeUnit.MILLISECONDS),
    maxBackoff = FiniteDuration(1000,TimeUnit.MILLISECONDS),
    randomFactor = 0.2
  ))

  import EthRpcJson._

  val cursor = new CursorBlock("BLOCK-eth")(config)
  val reorg = new ReorgBlock(config.blockReorg)
  implicit val uri = EthURI(config.feed,config.apiToken)
    
  override def source(feed:String) = {
    feed.split("://").toList match {
      case "http" :: _ | "https" :: _ | "eth" :: _  =>         
        
        val blockStr = 
          (config.block.split("://").toList match {
            case "file" :: file :: Nil => cursor.read(file)
            case "file" :: Nil => cursor.read()
            case _ => config.block
          })

        val blockStart = blockStr.strip match {
          case "latest" =>
            val json = s"""{
                "jsonrpc":"2.0","method":"eth_blockNumber",
                "params":[],
                "id": 0
              }""".trim.replaceAll("\\s+","")

            val rsp = requests.post(uri.uri, data = json,headers = Map("content-type" -> "application/json"))            
            
            rsp.statusCode match {
              case 200 => //
                val body = rsp.text()
                log.debug(s"${body}")

                try {
                  val r = ujson.read(rsp.text())
                  java.lang.Long.decode(r.obj("result").str).toLong

                } catch {
                  case e:Exception =>
                    log.error(s"failed to get block: ${body}",e)
                    sys.exit(3)
                    0
                }
              case _ => 
                log.error(s"failed to get latest block: ${rsp}")
                sys.exit(3)
                0
            }            

          case hex if hex.startsWith("0x") =>
            java.lang.Long.parseLong(hex.drop(2),16).toLong
          case dec =>
            dec.toLong
        }
        
        val blockEnd = config.blockEnd match {
          case "" => Int.MaxValue
          case "latest" => blockStart
          case hex if hex.startsWith("0x") =>
            java.lang.Long.parseLong(hex,16).toInt
          case _ @ dec =>
            dec.toInt
        }

        cursor.init(blockStart - config.blockLag, blockEnd)
                   
        log.info(s"cursor: ${cursor}")        

        val sourceTick = Source.tick(
          FiniteDuration(10,TimeUnit.MILLISECONDS), 
          //FiniteDuration(config.ingestCron.toLong,TimeUnit.SECONDS),
          FiniteDuration(config.throttle,TimeUnit.MILLISECONDS),
          s"${uri.uri}"
        )

        // ----- Reorg -----------------------------------------------------------------------------------
        val reorgFlow = (lastBlock:String) => {
          if(config.blockReorg > 0 ) {            
            
            val r = ujson.read(lastBlock)
            val result = r.obj("result").obj
            
            val blockNum = java.lang.Long.decode(result("number").str).toLong
            val blockHash = result("hash").str
            val ts = java.lang.Long.decode(result("timestamp").str).toLong
            val txCount = result("transactions").arr.size

            // check if reorg
            val rr = reorg.isReorg(blockNum,blockHash)
            if(rr.size > 0) {
              log.warn(s"reorg block: >>>>>>>>> ${blockNum}/${blockHash}: reorgs=${rr}")
              os.write.append(os.Path("REORG",os.pwd),s"${ts},${blockNum},${blockHash},${txCount}}")
              reorg.reorg(rr)
              true
            } else {
              
              reorg.cache(blockNum,blockHash,ts,txCount)              
            }

          } else true
        }
        
        // ------- Flow ------------------------------------------------------------------------------------
        sourceTick
          .map(h => {
            log.debug(s"Cron --> ${h}")

            // request latest block to know where we are from current            
            val json = s"""{
                "jsonrpc":"2.0","method":"eth_blockNumber",
                "params":[],
                "id": 0
              }""".trim.replaceAll("\\s+","")

            val rsp = requests.post(uri.uri, data = json,headers = Map("content-type" -> "application/json"))
            val body = rsp.text()
            //log.info(s"rsp=${rsp.statusCode}: ${body}")
            
            rsp.statusCode match {
              case 200 => //
                log.debug(s"${body}")
              case _ => 
                // retry
                log.error(s"RPC error: ${rsp.statusCode}: ${body}")
                throw new RetryException("")
            }
            
            val r = ujson.read(body)
            val lastBlock = java.lang.Long.decode(r.obj("result").str).toLong
            
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
          .takeWhile(blocks =>  // limit flow by the specified end block
            blocks.filter(_ <= blockEnd).size > 0
          )
          .mapConcat(blocks => {
            log.info(s"--> ${blocks}")
            
            val blocksReq = blocks.map(block => {
              val blockHex = s"0x${block.toHexString}"
              s"""{
                  "jsonrpc":"2.0","method":"eth_getBlockByNumber",
                  "params":["${blockHex}",true],
                  "id":0
                }""".trim.replaceAll("\\s+","")  
            })
                        
            val json = s"""[${blocksReq.mkString(",")}]"""
            val rsp = requests.post(uri.uri, data = json,headers = Map("content-type" -> "application/json"))
            
            log.info(s"rsp=${rsp.statusCode}")
            
            val body = rsp.text()
            
            rsp.statusCode match {
              case 200 => //
                log.trace(s"${body}")
              case _ => 
                // retry
                log.error(s"RPC error: ${rsp.statusCode}: ${body}")
                throw new RetryException("")
            }
            
            val batch = decodeBatch(body)
            batch
          })
          .filter(reorgFlow)
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

  def decodeReceipts(block: RpcBlock): Map[String,RpcReceipt] = {
    val b = block.result.get

    val ts = toLong(b.timestamp)
    val block_number = toLong(b.number)

    val json = 
    "[" + b.transactions.map( t => 
      s"""{"jsonrpc":"2.0","method":"eth_getTransactionReceipt","params":["${t.hash}"],"id":"${t.hash}"}"""
     ).mkString(",") +
    "]"
    .trim.replaceAll("\\s+","")

    log.info(s"transaction: ${b.transactions.size}")
      
    if(b.transactions.size > 0) {
      val receiptsRsp = requests.post(uri.uri, data = json,headers = Map("content-type" -> "application/json"))
      val receipts:Map[String,RpcReceipt] = receiptsRsp.statusCode match {
        case 200 =>
          // need to import it here for List[]

          val batchRsp = receiptsRsp.data.toString

          try {
            val batchReceipts = batchRsp.parseJson.convertTo[List[RpcReceiptResultBatch]]

            val rr:Seq[RpcReceipt] = batchReceipts.flatMap { r => 
              
              if(r.result.isDefined) {
                Some(r.result.get)
              } else {
                log.warn(s"could not get receipt: (tx=${r.id}): ${r}")
                None
              }
            }

            // commit cursor
            cursor.commit(toLong(b.number))

            rr.map( r => r.transactionHash -> r).toMap

          } catch {
            case e:Exception =>
              log.error(s"could not parse receipts batch: ${receiptsRsp}",e)
              Map()
          }
        case _ => 
          log.warn(s"could not get receipts batch: ${receiptsRsp}")
          Map()
      }
      receipts
    } else
      Map()    
  }
}
