package io.haas.ingest.solana.flow.rpc

import scala.util.{Success,Failure,Try}

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
import akka.actor.typed.ActorSystem
import akka.stream.RestartSettings
import scala.util.control.NoStackTrace
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.RestartSource
import akka.stream.Attributes

import requests.Response

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

import io.haas.core.RetryException
import io.haas.ingest.CursorBlock

import io.haas.ingest.solana.{TokBal,TokUI}

// ATTENTION !!!
// throttle is overriden in Config to support batchable retries !
abstract class PipelineSolana[T,O <: skel.Ingestable,E <: skel.Ingestable](config:Config)
  (implicit fmt:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E])
  extends PipelineIngest[T,O,E](config.copy(throttle = 0L))(fmt,parqEncoders,parsResolver) with SolanaDecoder[E] {
  
  import SolanaRpcJson._

  val cursor = new CursorBlock("BLOCK-solana")(config)
  implicit val uri:SolanaURI = SolanaURI(config.feed,config.apiToken)

  val encoding = config.options.getOrElse("encoding","jsonParsed")
  val rewards = config.options.getOrElse("rewards","false").toBoolean
  val commitment = config.options.getOrElse("commitment","finalized")
  val maxSupportedTransactionVersion = config.options.getOrElse("maxSupportedTransactionVersion","0").toInt
  val transactionDetails = config.options.getOrElse("transactionDetails","full")

  private val rpcHeaders: Map[String, String] = Map(
    "content-type" -> "application/json",
    // Ask server to gzip responses (requests-scala will transparently decompress).
    "accept-encoding" -> config.compression
  )
    
  override def source(feed:String) = {
    feed.split("://").toList match {
      case ("http" | "https" | SolanaURI.PREFIX | SolanaURI.PREFIX2 | SolanaURI.PREFIX_DEV | SolanaURI.PREFIX_DEV2 | SolanaURI.PREFIX_TEST | SolanaURI.PREFIX_TEST2) :: _ => 

        log.info(s"uri=${uri}")
        
        val blockStr = setCursorBlock(cursor,(txs: Seq[String]) => throw new Exception("not implemented"))
          // config.block.split("://").toList match {
          //   case "file" :: file :: Nil => cursor.setFile(file).read()
          //   case "file" :: Nil => cursor.read()
          //   case _ => config.block
          // }

        val blockStart:Long = blockStr.strip match {
          case "latest" =>
            val json = s"""{"jsonrpc":"2.0","method":"getLatestBlockhash","params":[{"commitment":"${commitment}"}],"id":1}"""
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
        
        // val blockEnd = config.blockEnd match {
        //   case "" => Int.MaxValue
        //   case "latest" => blockStart
        //   case hex if hex.startsWith("0x") =>
        //     java.lang.Long.parseLong(hex,16).toLong
        //   case _ @ dec =>
        //     dec.toLong
        // }
        val blockEnd = config.blockEnd match {
          case "" => 
            cursor.blockEnd
            //Int.MaxValue
          case "latest" => blockStart
          case hex if hex.startsWith("0x") =>
            java.lang.Long.parseLong(hex,16).toInt
          case _ @ dec =>
            dec.toLong
        }

        cursor.init(blockStart - config.blockLag, blockEnd)
                   
        log.info(s"cursor: ${cursor}")        

        val sourceTick = Source.tick(
          FiniteDuration(10,TimeUnit.MILLISECONDS),          
          FiniteDuration(config.throttle,TimeUnit.MILLISECONDS),          
          s"${uri.uri}"
        )
                
        // ------- Flow ------------------------------------------------------------------------------------
        val sourceFlow = sourceTick
          .map(h => {
            log.debug(s"Cron --> ${uri.uri}")
            
            //val json = s"""{"jsonrpc":"2.0","method":"getLatestBlockhash","params":[{"commitment":"finalized"}],"id": 0}"""
            //val json = s"""{"jsonrpc":"2.0","method":"getBlockHeight","id":1}"""
            val json = s"""{"jsonrpc":"2.0","method":"getSlot","params":[{"commitment":"${commitment}"}],"id":1}"""
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
                s"""{ "jsonrpc":"2.0","method":"getBlock", "params":[${block},{"encoding":"${encoding}","maxSupportedTransactionVersion":${maxSupportedTransactionVersion},"transactionDetails":"${transactionDetails}","rewards":${rewards} }], "id":${block} }"""
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
                // Solana may skip block (slots)
                //reorgFlow(b)
                true
              )
          })
          .throttle(1,FiniteDuration(config.blockThrottle,TimeUnit.MILLISECONDS)) // throttle fast range group           
          .filter(b => {
            if(b.contains(""""error":{"code":""")) {

              decodeError(b) match {
                case Success(err) if isMissing(err) =>
                  log.warn(s"Block[${err.id}]: missing")
                  // conitnue with next block
                  false
                  
                case _ =>
                  log.warn(s"Error: ${b}")
                  throw new RetryException("")
              }
            } else
              true
          })
          .map(b => ByteString(b))
        
        val sourceRestart = RestartSource.onFailuresWithBackoff(retrySettings.get) { () =>
          log.info(s"connect -> ${uri.uri}")
          sourceFlow
        }

        sourceRestart
          
      case _ => super.source(feed)
    }
  }

  def decodeError(b:String):Try[RpcError] = {
    Try(b.parseJson.convertTo[RpcError])
  }

  // """{"error":{"code":-32009,"message":"Slot 413393588 was skipped, or missing in long-term storage"},"id":413393588,"jsonrpc":"2.0"}"""
  def isMissing(err:RpcError):Boolean = {    
    err.error.code == -32009    
  }

  protected def accountPubKeys(keys: Array[JsValue]): Array[String] = {
    keys.map {
      case JsString(s) => s
      case JsObject(fields) =>
        fields.get("pubkey") match {
          case Some(JsString(s)) => s
          case _ => JsObject(fields).compactPrint
        }
      case other => other.compactPrint
    }
  }

  protected def toTokBal(tb: RpcPostTokenBalance): TokBal =
    TokBal(
      i = tb.accountIndex,
      pid = tb.programId,
      ui = TokUI(
        v = BigInt(tb.uiTokenAmount.amount),
        dec = tb.uiTokenAmount.decimals,
        // RpcUiTokenAmount.uiAmount is Double in the RPC model; keep it optional in domain.
        vu = Some(tb.uiTokenAmount.uiAmount),
        vs = Option(tb.uiTokenAmount.uiAmountString)
      ),
      mint = tb.mint,
      own = tb.owner
    )
}
