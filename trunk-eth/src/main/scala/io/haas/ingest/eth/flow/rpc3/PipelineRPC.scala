package io.haas.ingest.eth.flow.rpc3

import java.util.concurrent.atomic.AtomicLong

import scala.util.{Success,Failure,Try}
import scala.jdk.CollectionConverters._
import com.typesafe.scalalogging.Logger
import scala.concurrent.Future
import scala.concurrent.duration.{Duration,FiniteDuration}
import java.util.concurrent.TimeUnit

import akka.util.ByteString
import akka.http.scaladsl.model.{HttpRequest,HttpMethods,HttpEntity,ContentTypes}
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Flow
import akka.stream.Attributes
import akka.stream.OverflowStrategy
import akka.actor.typed.ActorSystem
import akka.stream.RestartSettings
import scala.util.control.NoStackTrace
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.RestartSource

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter

import spray.json._
import DefaultJsonProtocol._

import requests.Response

import com.github.mjakubowski84.parquet4s.{ParquetRecordEncoder,ParquetSchemaResolver}

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util
import io.syspulse.skel.config._

import io.syspulse.skel.ingest._
import io.syspulse.skel.ingest.store._
import io.syspulse.skel.ingest.flow.Pipeline
import io.syspulse.skel.ingest.flow.Flows
import io.syspulse.skel.blockchain.eth.EthUtil

import io.haas.ingest.eth.flow.rpc3._
import io.haas.ingest.eth.flow.rpc3.EthRpcJson._

import io.haas.ingest.eth.uri.{RpcURI,EthURI}
import io.haas.ingest.PipelineIngest
import io.haas.ingest.eth

import io.haas.ingest.Config

import io.haas.core.RetryException
import io.haas.ingest.CursorBlock
import io.haas.reorg.{ReorgBlock,ReorgBlock1,ReorgBlock2}

// ATTENTION !!!
// throttle is overriden in Config to support batchable retries !
abstract class PipelineRPC[T,O <: skel.Ingestable,E <: skel.Ingestable]
    (config:Config)
    (implicit fmt:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E])
  extends PipelineIngest[T,O,E](config.copy(throttle = 0L))(fmt,parqEncoders,parsResolver) 
  with RPCDecoder[E] {

  import EthRpcJson._

  private val rpcHeaders: Map[String, String] = Map(
    "content-type" -> "application/json",
    // Ask server to gzip responses (requests-scala will transparently decompress).
    "accept-encoding" -> config.compression
  )

  val cursor = new CursorBlock()(config)
  implicit val uri:RpcURI = EthURI(config.feed,config.apiToken)
  
  val reorg = config.reorgFlow match {
    case "reorg1" => new ReorgBlock1(config.blockReorg)
    case "reorg2" => new ReorgBlock2(config.blockReorg)
    case _ => new ReorgBlock2(config.blockReorg)
  }
        
  // ----- Source ----------------------------------------------------------------------------------------------------------------
  override def source(feed:String) = {
    feed.split("://").toList match {
      case _ if(uri.uri != "")  =>         
        
        val blockStr = setCursorBlock(cursor,(txs: Seq[String]) => decodeBlocks[Long](txs,tx => EthUtil.toLong(tx.blockNumber))(config,uri.uri))

        val blockStart = blockStr.strip match {
          case "latest" =>
            val json = s"""{"jsonrpc":"2.0","method":"eth_blockNumber", "params":[],"id":0}"""

            val rsp = {
              log.info(s"Latest -> ${uri.uri}")

              var rsp:Option[requests.Response] = None
              while(!rsp.isDefined)  {
                rsp = try {
                  Some(requests.post(uri.uri, data = json,headers = rpcHeaders))
                } catch {
                  case e:Exception => 
                    log.error(s"failed to request latest block: ${uri.uri}",e)
                    Thread.sleep(config.throttle)
                    None
                }              
              } 
              rsp.get
            }
            
            rsp.statusCode match {
              case 200 => //
                val body = rsp.text()
                log.debug(s"body=${body}")

                try {
                  val r = ujson.read(rsp.text())
                  java.lang.Long.decode(r.obj("result").str).toLong

                } catch {
                  case e:Exception =>
                    log.error(s"failed to decode block: ${body}",e)
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
        
        // ----- Reorg Subflow -----------------------------------------------------------------------------
        val reorgFlow = (lastBlock:String) => {
          if(config.blockReorg > 0 ) { 
            val (fresh,_) = reorg.track(lastBlock)
            fresh

          } else true
        }
                
        // ------- Flow ------------------------------------------------------------------------------------
        val sourceFlow = 
          sourceTick
          .map(h => {            

            // request latest block to know where we are from current            
            val json = s"""{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id": 0}"""

            log.debug(s"Cron: ${h}: ${json} -> ${uri.uri}")

            val rsp = requests.post(uri.uri, data = json,headers = Map("content-type" -> "application/json"))
            val body = rsp.text()            
            
            rsp.statusCode match {
              case 200 => //
                log.debug(s"${body}")
              case _ => 
                // retry
                log.error(s"RPC error: ${rsp.statusCode}: ${body}")
                throw new RetryException("")
            }
            
            val r = ujson.read(body)
            val lastBlock = try {
              java.lang.Long.decode(r.obj("result").str).toLong
            } catch {
              case e:Exception =>
                log.error(s"Failed to decode last block: '${body}': ${e.getMessage}")
                throw e
            }
            
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
                reorg.range(cursor.get(),lastBlock)
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
            // filter until the last block
            val blocks0 = blocks.filter(_ <= blockEnd)
            if(blocks0.size == 0) {
              Seq.empty
            } else {
              log.info(s"--> ${blocks0.toVector}")

              val blocks1 = if(cursor.last() != 0) 
                blocks0.filter(_ > cursor.last())
              else
                blocks0
              
              val blocks2 = if(blocks1.size != blocks0.size && cursor.last() != 0) {
                val blocksOld = blocks0.filter(_ < cursor.last())
                log.warn(s">>>> PAST=${blocksOld}: last=${cursor.last()}")
                blocks1
              } else
                blocks1

              val blocks = blocks2
              
              // log.info(s"--> ${blocks.toVector}")

              // if limit is specified, take the last limit
              val blockForget = if(config.blockLimit > 0) blocks.takeRight(config.blockLimit) else blocks

              val ts0 = System.currentTimeMillis()
              val blocksReq = blockForget
                .map(block => {
                  val blockHex = s"0x${block.toHexString}"
                  s"""{"jsonrpc":"2.0","method":"eth_getBlockByNumber","params":["${blockHex}",true],"id":0}"""
                })

              // if only 1 tx, don't batch (to be compatible with some weird RPC which don't support batch)
              val json = if(blocks.size == 1) 
                blocksReq.head 
              else 
                s"""[${blocksReq.mkString(",")}]"""

              val batch = try {
                val rsp = requests.post(uri.uri, data = json,headers = rpcHeaders)
                val body = rsp.text()
                
                rsp.statusCode match {
                  case 200 => //
                    log.trace(s"${body}")
                  case _ => 
                    // retry
                    log.error(s"RPC error: ${rsp.statusCode}: ${body}")
                    throw new RetryException(s"${rsp.statusCode}")
                }
                                
                val batch = if(blocks.size == 1)
                  decodeSingle(body)
                else
                  decodeBatch(body)
                
                val ts1 = System.currentTimeMillis()
                log.info(s"--> ${blocks0.toVector} ${ts1 - ts0}ms")

                batch

              } catch {
                case e:Exception =>
                  log.error(s"failed to get batch: '${json}'",e)
                  Seq()
              }

              batch
            }
          })          
          // .log(s"Source: feed=${feed}")
          .throttle(1,FiniteDuration(config.blockThrottle,TimeUnit.MILLISECONDS)) // throttle fast range group 
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
                reorgFlow(b)
              )
          }) 
          .map(b => ByteString(b))
      
        // restarter for source 
        val sourceRestart = RestartSource.onFailuresWithBackoff(retrySettings.get) { () =>
          log.info(s"connect -> ${uri.uri}")
          sourceFlow
        }

        sourceRestart

      case _ => super.source(feed)
    }
  }
    
}
