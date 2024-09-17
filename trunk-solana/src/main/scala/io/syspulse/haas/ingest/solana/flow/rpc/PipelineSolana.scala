package io.syspulse.haas.ingest.solana.flow.rpc

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

import io.syspulse.haas.ingest.solana.flow.rpc._
import io.syspulse.haas.ingest.solana.flow.rpc.SolanaRpcJson._

import io.syspulse.haas.ingest.solana.SolanaURI
import io.syspulse.haas.ingest.PipelineIngest
import io.syspulse.haas.ingest.solana

import io.syspulse.haas.ingest.Config

import akka.actor.typed.ActorSystem
import akka.stream.RestartSettings
import scala.util.control.NoStackTrace
import requests.Response
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.RestartSource

import io.syspulse.haas.core.RetryException
import io.syspulse.haas.ingest.CursorBlock

// ATTENTION !!!
// throttle is overriden in Config to support batchable retries !
abstract class PipelineSolana[T,O <: skel.Ingestable,E <: skel.Ingestable](config:Config)
                                                                       (implicit fmt:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E])
  extends PipelineIngest[T,O,E](config.copy(throttle = 0L))(fmt,parqEncoders,parsResolver) with SolanaDecoder[E] {
  
  import SolanaRpcJson._

  val cursor = new CursorBlock("BLOCK-solana")(config)
  implicit val uri = SolanaURI(config.feed,config.apiToken)
    
  override def source(feed:String) = {
    feed.split("://").toList match {
      case ("http" | "https" | "sol" | "solana" | "sol:dev" | "solana:dev" | "sol:test" | "solana:test") :: _ => 

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
            val rsp = requests.post(uri.uri,
              headers = Seq(("Content-Type","application/json")),
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
            val json = s"""{"jsonrpc":"2.0","method":"getLatestBlockhash","params":[{"commitment":"finalized"}],"id": 0}""".trim.replaceAll("\\s+","")
            //val json = s"""{"jsonrpc":"2.0","method":"getBlockHeight","id":1}"""
            val rsp = requests.post(uri.uri, data = json,headers = Map("content-type" -> "application/json"))
            
            rsp.statusCode match {
              case 200 => //
              case _ => 
                // retry
                log.error(s"RPC error: ${rsp.statusCode}: ${rsp.text()}")
                throw new RetryException("")
            }
            
            val r = ujson.read(rsp.text())
            //val lastBlock = r.obj("result").obj("value").obj("lastValidBlockHeight").num.toLong
            val lastBlock = r.obj("result").obj("context").obj("slot").num.toLong
            //val lastBlock = r.obj("result").num.toLong
            
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
          .takeWhile(blocks => // limit flow by the specified end block
            blocks.filter(_ <= blockEnd).size > 0
          )
          .map(blocks => {
            log.info(s"--> ${blocks}")
            
            val blocksReq = blocks.map(block => {              
              // ATTENTION: block is slot !!!
              s"""{
                  "jsonrpc":"2.0","method":"getBlock\",
                  "params":[${block},{"encoding":"json","maxSupportedTransactionVersion":0,"transactionDetails":"full","rewards":false }],
                  "id":0
                }""".trim.replaceAll("\\s+","")
            })
                        
            val json = s"""[${blocksReq.mkString(",")}]"""
            val rsp = requests.post(uri.uri, data = json,headers = Map("content-type" -> "application/json"))            
            val body = rsp.text()
            
            rsp.statusCode match {
              case 200 => //
                log.debug(s"body=${body}")                

              case _ => 
                // retry
                log.error(s"RPC error: ${rsp.statusCode}: ${body}")
                throw new RetryException("")
            }
            
            val batch = decodeBatch(body)
            batch
          })
          .log(s"${feed}")
          .throttle(1,FiniteDuration(config.blockThrottle,TimeUnit.MILLISECONDS)) // throttle fast range group 
          .mapConcat(batch => batch)
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

  // ---- Receipts --------------------------------------------------------------------------------------------------------------------------
  
  // legacy and very inefficient for the whole Block  
  // def decodeReceiptsBatch(block: RpcBlock): Map[String,RpcReceipt] = {
  //   decodeTxReceipts(block.transactions.map(_.transaction_hash).toIndexedSeq)
  // }

  // def decodeTxReceipts(transactions: Seq[String]): Map[String,RpcReceipt] = {
    
  //   if(transactions.size == 0)
  //     return Map()
    
  //   val receiptBatch = if(config.receiptBatch == -1) transactions.size else config.receiptBatch
  //   val ranges = transactions.grouped(receiptBatch).toSeq

  //   val receiptMap = ranges.view.zipWithIndex.map{ case(range,i) => {      
  //     //log.debug(s"transactions: ${b.transactions.size}")
        
  //     if(range.size > 0) {
  //       if(i != 0) {
  //         Thread.sleep(config.receiptThrottle)
  //       }

  //       val json = 
  //         "[" + range.map( txHash => 
  //           s"""{"jsonrpc":"2.0","method":"solana_getTransactionReceipt","params":["${txHash}"],"id":"${txHash}"}"""
  //         ).mkString(",") +
  //         "]"
  //         .trim.replaceAll("\\s+","")
          
  //       try {
  //         val receiptsRsp = requests.post(uri.uri, data = json,headers = Map("content-type" -> "application/json"))        
  //         val receipts:Seq[(String,RpcReceipt)] = receiptsRsp.statusCode match {
  //           case 200 =>
              
  //             val batchRsp = receiptsRsp.text()//receiptsRsp.data.toString
              
  //             try {
  //               if(batchRsp.contains("""error""") && batchRsp.contains("""code""")) {
  //                 throw new Exception(s"${batchRsp}")
  //               }
              
  //               val batchReceipts = batchRsp.parseJson.convertTo[List[RpcReceiptResult]]

  //               val rr:Seq[RpcReceipt] = batchReceipts.flatMap { r => 
                  
  //                 if(r.result.isDefined) {
  //                   Some(r.result.get)
  //                 } else {
  //                   log.warn(s"could not get receipt: (tx=${r.id}): ${r}")
  //                   None
  //                 }
  //               }
                
  //               rr.map( r => r.transaction_hash -> r).toSeq              

  //             } catch {
  //               case e:Exception =>
  //                 log.error(s"could not parse receipts batch: ${receiptsRsp}",e)
  //                 Seq()
  //             }
  //           case _ => 
  //             log.warn(s"could not get receipts batch: ${receiptsRsp}")
  //             Seq()
  //         }
  //         receipts
  //       } catch {
  //         case e:Exception =>
  //           log.error("failed to get receipts",e)
  //           Map()
  //       }

  //     } else
  //       Map()  
  //   }}.flatten.toMap 
    
  //   receiptMap
  // }

  // def decodeReceipts(block: RpcBlock): Map[String,RpcReceipt] = {
  //   config.receiptRequest match {
  //     case "block" => decodeReceiptsBlock(block)
  //     case "batch" => decodeReceiptsBatch(block)
  //     case _ => decodeReceiptsBlock(block)
  //   }
  // }
  
  // // --- Receipts via one call
  // def decodeReceiptsBlock(block: RpcBlock): Map[String,RpcReceipt] = {
  //   val b = block

  //   if(b.transactions.size == 0)
  //     return Map()
            
  //   val receiptMap = {      
  //     //log.debug(s"transactions: ${b.transactions.size}")        

  //     val id = b.block_number
  //     val json =  s"""{"jsonrpc":"2.0","method":"solana_getBlockWithReceipts","params":["${b.block_number}"],"id":"${id}"}"""
  //       .trim.replaceAll("\\s+","")
        
  //     try {
  //       val receiptsRsp = requests.post(uri.uri, data = json,headers = Map("content-type" -> "application/json"))
  //       val receipts:Seq[(String,RpcReceipt)] = receiptsRsp.statusCode match {
  //         case 200 =>
            
  //           val rsp = receiptsRsp.text()
            
  //           try {
  //             if(rsp.contains("""error""") && rsp.contains("""code""")) {
  //               throw new Exception(s"${b.block_number}: ${rsp}")
  //             }
            
  //             val rr = rsp.parseJson.convertTo[RpcBlockReceiptsResult].result
  //             rr.map( r => r.transaction_hash -> r).toSeq

  //           } catch {
  //             case e:Exception =>
  //               log.error(s"failed to parse receipts: ${b.block_number}: ${receiptsRsp}: rsp=${rsp}",e)
  //               Seq()
  //           }
  //         case _ => 
  //           log.warn(s"failed to get receipts: ${b.block_number}: ${receiptsRsp}")
  //           Seq()
  //       }
  //       receipts
  //     } catch {
  //       case e:Exception =>
  //         log.error(s"failed to get block receipts: ${b.block_number}",e)
  //         Map()
  //     }

  //   }.toMap 
    
  //   receiptMap
  // }
}
