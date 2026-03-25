package io.haas.ingest.solana.flow.rpc

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

import io.haas.ingest.solana.flow.rpc._
import io.haas.ingest.solana.flow.rpc.SolanaRpcJson._

import io.haas.ingest.solana.SolanaURI
import io.haas.ingest.PipelineIngest
import io.haas.ingest.solana

import io.haas.ingest.Config

import akka.actor.typed.ActorSystem
import akka.stream.RestartSettings
import scala.util.control.NoStackTrace
import requests.Response
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.RestartSource

import io.haas.core.RetryException
import io.haas.ingest.CursorBlock
import akka.stream.Attributes

// ATTENTION !!!
// throttle is overriden in Config to support batchable retries !
abstract class PipelineSolana[T,O <: skel.Ingestable,E <: skel.Ingestable](config:Config)
                                                                       (implicit fmt:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E])
  extends PipelineIngest[T,O,E](config.copy(throttle = 0L))(fmt,parqEncoders,parsResolver) with SolanaDecoder[E] {
  
  import SolanaRpcJson._

  val cursor = new CursorBlock("BLOCK-solana")(config)
  implicit val uri = SolanaURI(config.feed,config.apiToken)

  val encoding = "jsonParsed"
  val compression = "gzip"

  private val rpcHeaders: Map[String, String] = Map(
    "content-type" -> "application/json",
    // Ask server to gzip responses (requests-scala will transparently decompress).
    "accept-encoding" -> compression
  )
    
  override def source(feed:String) = {
    feed.split("://").toList match {
      case ("http" | "https" | SolanaURI.PREFIX | SolanaURI.PREFIX2 | SolanaURI.PREFIX_DEV | SolanaURI.PREFIX_DEV2 | SolanaURI.PREFIX_TEST | SolanaURI.PREFIX_TEST2) :: _ => 

        log.info(s"uri=${uri}")
        
        val blockStr = config.block.split("://").toList match {
          case "file" :: file :: Nil => cursor.setFile(file).read()
          case "file" :: Nil => cursor.read()
          case _ => config.block
        }

        val blockStart:Long = blockStr.strip match {
          case "latest" =>
            val json = s"""{"jsonrpc":"2.0","method":"getLatestBlockhash","params":[{"commitment":"finalized"}],"id":1}"""
            //val json = s"""{"jsonrpc":"2.0","method":"getBlockHeight","id":1}"""
            log.debug(s"${json} -> ${uri.uri}")
            
            val rsp = requests.post(uri.uri,
              headers = rpcHeaders,
              data = json
            )
            
            if(rsp.statusCode != 200) {
              log.error(s"failed to get latest block: ${rsp}")
              0
            } else {
              val r = ujson.read(rsp.text())
              //r.obj("result").obj("context").obj("lastValidBlockHeight").num.toLong
              r.obj("result").obj("context").obj("slot").num.toLong
              //r.obj("result").num.toLong
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
          s"ingest-solana-${feed}"
        )
                
        // ------- Flow ------------------------------------------------------------------------------------
        val sourceFlow = sourceTick
          .map(h => {
            log.debug(s"Cron --> ${uri.uri}")

            // request latest block to know where we are from current
            val blockHex = "latest"
            //val json = s"""{"jsonrpc":"2.0","method":"getLatestBlockhash","params":[{"commitment":"finalized"}],"id": 0}"""
            //val json = s"""{"jsonrpc":"2.0","method":"getBlockHeight","id":1}"""
            val json = """{"jsonrpc":"2.0","method":"getSlot","params":[{"commitment":"finalized"}],"id":1}"""
            log.debug(s"${json} -> ${uri.uri}")
            val rsp = requests.post(uri.uri, data = json, headers = rpcHeaders)
            
            rsp.statusCode match {
              case 200 => //
              case _ => 
                // retry
                log.error(s"RPC error: ${rsp.statusCode}: ${rsp.text()}")
                throw new RetryException("")
            }
            
            val r = ujson.read(rsp.text())
            //val lastBlock = r.obj("result").obj("value").obj("lastValidBlockHeight").num.toLong
            //val lastBlock = r.obj("result").obj("context").obj("slot").num.toLong
            //val lastBlock = r.obj("result").num.toLong
            val lastBlock = r.obj("result").num.toLong
            
            //log.info(s"last=${lastBlock}, current=${cursor.get()}, lag=${config.blockLag}")
            val currentBlock = cursor.get()
            log.info(s"Cursor: last=${lastBlock}, current=${currentBlock}, distance=${lastBlock - currentBlock}, lag=${config.blockLag}, reorg=${config.blockReorg}")
            lastBlock - config.blockLag
          })
          .map(lastBlock => {
            // ATTENTION:
            // lag and reorg are not compatible !

            val bb = 
              if(cursor.blockList.size > 0) {
                // selected list
                cursor.getList()
              }
              else
              if(config.blockReorg == 0 || cursor.get() < (lastBlock - config.blockReorg))              
                // normal fast operation or reorg before the tip
                cursor.get() to lastBlock
              else {
                // reorg operation on the tip
                // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!11
                log.error(s"reorg operation on the tip: ${cursor.get()} -> ${lastBlock}")
                //reorg.range(cursor.get(),lastBlock)
                Seq()
              }
            
            bb.grouped(config.blockBatch)
            //bb.take(config.blockBatch)
          })
          // flatted seq of batchs to stream of batches
          .mapConcat(bb => bb)

          // limit flow by the specified end block
          .takeWhile(blocks => {
            //blocks.filter(_ <= blockEnd).size > 0
            blockEnd == Long.MaxValue ||
            blocks.size == 0 ||              
            blocks.find(_ <= blockEnd).isDefined
            
          })
          .map(blocks => {
            log.info(s"--> ${blocks}")

            val blocksReq = blocks
              .takeRight(if(config.blockLimit > 0) config.blockLimit else blocks.size)
              .map(block => {              
                // ATTENTION: block is slot !!!
                s"""{ "jsonrpc":"2.0","method":"getBlock", "params":[${block},{"encoding":"${encoding}","maxSupportedTransactionVersion":0,"transactionDetails":"full","rewards":false }], "id":${block} }"""
              })
            
                        
            //val json = if(config.blockLimit > 1) s"""[${blocksReq.mkString(",")}]""" else blocksReq.head
            val json = s"""[${blocksReq.mkString(",")}]"""

            log.debug(s"${json} -> ${uri.uri}")
            val rsp = requests.post(uri.uri, data = json, headers = rpcHeaders)
            val body = rsp.text()
            
            rsp.statusCode match {
              case 200 => //
                log.debug(s"body=${body}")

              case _ => 
                // retry
                log.error(s"RPC error: ${rsp.statusCode}: ${body}")
                throw new RetryException("")
            }
            
            //val batch = if(config.blockLimit > 1) decodeBatch(body) else decodeSingle(body)
            val batch = decodeBatch(body)
            batch
          })
          //.throttle(1,FiniteDuration(config.blockThrottle,TimeUnit.MILLISECONDS)) // throttle fast range group 
          .log(s"Source: feed=${feed}")
          .addAttributes(
            Attributes.logLevels(
              onElement = Attributes.LogLevels.Off,
              onFinish = Attributes.LogLevels.Warning,
              onFailure = Attributes.LogLevels.Error))
          // range -> blocks stream          
          .mapConcat(batch => {
            batch
              .filter(b => 
                // process reorgs here
                // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                //reorgFlow(b)
                true
              )
          })
          .throttle(1,FiniteDuration(config.blockThrottle,TimeUnit.MILLISECONDS)) // throttle fast range group           
          .map(b => {
            if(b.contains(""""error":{"code":""")) {
              log.warn(s"${b}")
              throw new RetryException("")

            } else
              ByteString(b)
          })
        
        val sourceRestart = RestartSource.onFailuresWithBackoff(retrySettings.get) { () =>
          log.info(s"connect -> ${uri.uri}")
          sourceFlow
        }

        sourceRestart
          
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
