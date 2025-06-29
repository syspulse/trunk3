package io.syspulse.haas.ingest.eth.flow.rpc3

import java.util.concurrent.atomic.AtomicLong
import io.syspulse.skel.ingest.flow.Flows

import scala.util.{Success,Failure}
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
import akka.stream.scaladsl.RestartSource

import io.syspulse.haas.core.RetryException
import io.syspulse.haas.ingest.CursorBlock
import io.syspulse.haas.reorg.{ReorgBlock,ReorgBlock1,ReorgBlock2}
import akka.stream.Attributes
import akka.stream.OverflowStrategy
import scala.concurrent.Future

// ATTENTION !!!
// throttle is overriden in Config to support batchable retries !
abstract class PipelineRPC[T,O <: skel.Ingestable,E <: skel.Ingestable]
    (config:Config)
    (implicit fmt:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E])
  extends PipelineIngest[T,O,E](config.copy(throttle = 0L))(fmt,parqEncoders,parsResolver) 
  with RPCDecoder[E] {

  import EthRpcJson._

  val cursor = new CursorBlock("BLOCK-eth")(config)
  
  val reorg = config.reorgFlow match {
    case "reorg1" => new ReorgBlock1(config.blockReorg)
    case "reorg2" => new ReorgBlock2(config.blockReorg)
    case _ => new ReorgBlock2(config.blockReorg)
  }

  implicit val uri = EthURI(config.feed,config.apiToken)
      
  // ----- Source ----------------------------------------------------------------------------------------------------------------
  override def source(feed:String) = {
    feed.split("://").toList match {
      case "http" :: _ | "https" :: _ | "eth" :: _  =>         
        
        val blockStr = 
          (config.block.split("://").toList match {
            // start from latest and save to file
            case "latest" :: file :: Nil => cursor.setFile(file).read()
              "latest"
            case "last" :: file :: Nil => cursor.setFile(file).read()
              "latest"
            case "latest" :: Nil =>  // use default file
              cursor.setFile("").read()
              "latest"

            case "file" :: file :: Nil => cursor.setFile(file).read()
            case "file" :: Nil => cursor.read()

            case "list" :: file :: Nil => 
              val data = os.read(os.Path(file,os.pwd))
              val list = data.split("[\\n,]").filter(!_.isBlank).map(_.trim.toLong)
              cursor.setList(list.toSeq)
              list.head.toString

            // start block and save to file (10://file.txt)
            case block :: file :: Nil => cursor.setFile(file).read(); 
              block

            case _ => config.block
          })

        val blockStart = blockStr.strip match {
          case "latest" =>
            val json = s"""{
                "jsonrpc":"2.0","method":"eth_blockNumber",
                "params":[],
                "id": 0
              }""".trim.replaceAll("\\s+","")

            val rsp = {
              log.info(s"Latest -> ${uri.uri}")

              var rsp:Option[requests.Response] = None
              while(!rsp.isDefined)  {
                rsp = try {
                  Some(requests.post(uri.uri, data = json,headers = Map("content-type" -> "application/json")))
                } catch {
                  case e:Exception => 
                    log.error(s"request latest block failed -> ${uri.uri}",e)
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
          case "" => 
            cursor.blockEnd
            //Int.MaxValue
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
        .conflate((lastMessage, newMessage) => newMessage)

        // val sourceTick = Source.repeat(())
        //   .throttle(1, FiniteDuration(config.throttle,TimeUnit.MILLISECONDS))          
        
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
            
            //bb
            //bb.grouped(config.blockBatch)
            bb.take(config.blockBatch)
          })          
          //.groupedWithin(config.blockBatch, FiniteDuration(1L, TimeUnit.MILLISECONDS))          
          // .map(blocks => {
          //   log.info(s"-> ${blocks}")

          //   val bb = if(config.blockReorg == 0) {
          //     // distinct and checking for current commit this is needed because of backpressure in groupedWithin when Sink is restarted (like Kafka reconnect)
          //     // when downstream backpressur is working, it generated for every Cron tick a new Range which produces
          //     // duplicates since commit is not changing. 
          //     // Example: 
          //     // PipelineRPC.scala:237] --> Vector(61181547, 61181548, 61181549, 61181547, 61181548)
          //     // PipelineRPC.scala:237] --> Vector(61181549, 61181550, 61181547, 61181548, 61181549)
          //     blocks
          //       .distinct
          //       .filter(b => b <= blockEnd && b >= cursor.get())
          //   } else
          //     blocks

          //   // when retrieving blocks in range, it is not a race
          //   if(bb.size == 0 && cursor.blockEnd == Int.MaxValue) {
          //     // informational
          //     log.warn(s">>>> Race: ${blocks} -> ${bb}: cursor=${cursor.get()}")
          //   }
            
          //   bb
          // })
          // .filter(_.size > 0)       // can be empty due no new elements from RPC
          // .takeWhile(blocks => {    // limit flow by the specified end block
          //   blocks.filter(_ <= blockEnd).size > 0            
          // })

          // limit flow by the specified end block
          .takeWhile(blocks => {
            //blocks.filter(_ <= blockEnd).size > 0
            blockEnd == Long.MaxValue ||
            blocks.size == 0 ||              
            blocks.find(_ <= blockEnd).isDefined
            
          })
          .map(blocks0 => {
            if(blocks0.size == 0) {
              Seq.empty
            } else {
              log.info(s"--> ${blocks0.toVector}")

              val blocks1 = if(cursor.last() != 0) 
                blocks0.filter(_ > cursor.last())
              else
                blocks0
              
              val blocks = if(blocks1.size != blocks0.size && cursor.last() != 0) {
                val blocksOld = blocks0.filter(_ < cursor.last())
                log.warn(s">>>> PAST=${blocksOld}: last=${cursor.last()}")
                blocks1
              } else
                blocks1
              

              // if limit is specified, take the last limit
              val blockForget = if(config.blockLimit > 0) blocks.takeRight(config.blockLimit) else blocks

              val ts0 = System.currentTimeMillis()
              val blocksReq = blockForget
                .map(block => {
                  val blockHex = s"0x${block.toHexString}"
                  s"""{
                      "jsonrpc":"2.0","method":"eth_getBlockByNumber",
                      "params":["${blockHex}",true],
                      "id":0
                    }""".trim.replaceAll("\\s+","")  
                })

              // if only 1 tx, don't batch (to be compatible with some weird RPC which don't support batch)
              val json = if(blocks.size == 1) 
                blocksReq.head 
              else 
                s"""[${blocksReq.mkString(",")}]"""

              val batch = try {
                val rsp = requests.post(uri.uri, data = json,headers = Map("content-type" -> "application/json"))                        
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
          // process reorgs here
          .mapConcat(batch => {
            batch
              .filter(b => reorgFlow(b))
          }) 
          // .filter(
          //   reorgFlow
          // )
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

  // override def sink() = {
  //   super
  //   .sink()
  //   .recover {
  //     case e: RuntimeException => e.getMessage
  //   }
  // }

  def decodeSingle(rsp:String):Seq[String] = Seq(rsp)

  def decodeBatch(rsp:String):Seq[String] = {
    // ATTENTION !!!
    // very inefficient, optimize with web3-proxy approach 
    val jsonBatch = ujson.read(rsp)
    jsonBatch.arr.map(a => a.toString()).toSeq
  }
    
}
