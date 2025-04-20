package io.syspulse.haas.ingest.bitcoin.flow.rpc

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

import io.syspulse.haas.ingest.PipelineIngest
import io.syspulse.haas.ingest.bitcoin

import io.syspulse.haas.ingest.Config

import akka.actor.typed.ActorSystem
import akka.stream.RestartSettings
import scala.util.control.NoStackTrace
import requests.Response
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.RestartSource

import io.syspulse.haas.core.RetryException
import io.syspulse.haas.ingest.CursorBlock

import io.syspulse.haas.ingest.bitcoin.flow.rpc.BitcoinRpc
import io.syspulse.haas.ingest.bitcoin.BitcoinURI
import io.syspulse.haas.reorg.{ReorgBlock,ReorgBlock1,ReorgBlock2}

// ATTENTION !!!
// throttle is overriden in Config to support batchable retries !
abstract class PipelineRPC[T,O <: skel.Ingestable,E <: skel.Ingestable]
    (config:Config)
    (implicit fmt:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E])
  extends PipelineIngest[T,O,E](config.copy(throttle = 0L))(fmt,parqEncoders,parsResolver) {

  protected val log = Logger(getClass)

  val cursor = new CursorBlock("BLOCK-bitcoin")(config)
  
  val reorg = config.reorgFlow match {
    case _ => new ReorgBlock2(config.blockReorg)
  }

  implicit val uri = BitcoinURI(config.feed)
  implicit val rpc = new BitcoinRpc(uri.url)

  // ----- Source ----------------------------------------------------------------------------------------------------------------
  override def source(feed:String) = {
    feed.split("://").toList match {
      case "http" :: _ | "https" :: _ | "bitcoin" :: _  =>
        
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
            askLastBlock(uri)            

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
          s"${uri.url}"
        )

        // ----- Reorg Subflow -----------------------------------------------------------------------------
        val reorgFlow = (lastBlock:String) => {
          if(config.blockReorg > 0 ) { 
            val (fresh,_) = reorg.track(lastBlock)
            fresh

          } else true
        }

        // ------- Flow ------------------------------------------------------------------------------------
        val sourceFlow = sourceTick
          .map(h => {
            log.debug(s"Cron --> ${h}")

            // request latest block to know where we are from current            
            val lastBlock = askLastBlock(uri)
            
            log.info(s"last=${lastBlock}, current=${cursor.get()}, lag=${config.blockLag}, reorg=${config.blockReorg}")
            lastBlock - config.blockLag
          })
          .mapConcat(lastBlock => {
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
            
            bb
          })          
          .groupedWithin(config.blockBatch,FiniteDuration(1L,TimeUnit.MILLISECONDS)) // batch limiter 
          .map(blocks => {
            
            val bb = if(config.blockReorg == 0) {
              blocks
              .distinct
              .filter(b => b <= blockEnd && b >= cursor.get())                
            } else
              blocks

            // when retrieving blocks in range, it is not a race
            if(bb.size == 0 && cursor.blockEnd == Int.MaxValue) {
              // informational
              log.warn(s"Race: ${blocks} -> ${bb}: cursor=${cursor.get()}")
            }
            bb
          })
          .filter(_.size > 0)       // due to parallelization and slow downstream processing, race is possible and empty list is passed
          .takeWhile(blocks => {    // limit flow by the specified end block
            blocks.filter(_ <= blockEnd).size > 0            
          })
          .map(blocks => {
            log.info(s"--> ${blocks}")

            val blockHashes = {
              askBlockHashes(uri,if(config.blockLimit > 0) blocks.takeRight(config.blockLimit) else blocks)
            }

            // if limit is specified, take the last limit
            val batch = {
              askBlocks(uri,blockHashes)
            }
              
            batch

          })
          .log(s"${feed}")
          .throttle(1,FiniteDuration(config.blockThrottle,TimeUnit.MILLISECONDS)) // throttle fast range group 
          .mapConcat(batch => batch)
          .filter(
            // it must return True to continue processing or false (when duplicated due to reorg algo)
            reorgFlow
          )
          .map(b => ByteString(b))
      
        val sourceRestart = RestartSource.onFailuresWithBackoff(retrySettings.get) { () =>
          log.info(s"Connect -> ${uri.url}")
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

  def askLastBlock(uri:BitcoinURI):Long = {
    val url = uri.url
    val json = s"""{
        "jsonrpc":"1.0","method":"getblockchaininfo",
        "params":[],
        "id": 0
      }""".trim.replaceAll("\\s+","")

    log.debug(s"${json} -> ${url}")

    val rsp = requests.post(url, data = json,headers = Map("content-type" -> "application/json"))
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
    
    try {
      val r = ujson.read(rsp.text())
      val result = r.obj("result")
      val lastBlockHash = result.obj("bestblockhash").str
      val lastBlock = result.obj("blocks").num

      lastBlock.toLong
    } catch {
      case e:Exception =>
        log.error(s"failed to get last block: ${body}",e)
        throw e
    }
  }

  def askBlockHashes(uri:BitcoinURI,blocks:Seq[Long]):Seq[String] = {    
    val url = uri.url
    val blocksReq =blocks
      .map(block => {
        s"""{
            "jsonrpc":"1.0","method":"getblockhash",
            "params":[${block}],
            "id":0
          }""".trim.replaceAll("\\s+","")  
      })

    // if only 1 tx, don't batch (to be compatible with some weird RPC which don't support batch)
    val json = if(blocks.size == 1) 
      blocksReq.head 
    else 
      s"""[${blocksReq.mkString(",")}]"""    

    try {
      log.debug(s"${json} -> ${url}")

      val rsp = requests.post(url, data = json,headers = Map("content-type" -> "application/json"))                        
      val body = rsp.text()
      
      rsp.statusCode match {
        case 200 => //
          log.trace(s"${body}")
        case _ => 
          // retry
          log.error(s"failed to get hashes: ${rsp.statusCode}: ${body}")
          throw new RetryException(s"${rsp.statusCode}")
      }
                      
      val batch = if(blocks.size == 1)
        decodeSingle(body)
      else
        decodeBatch(body)
      
      batch.map(b => {
        ujson.read(b).obj("result").str
      }).toSeq      

    } catch {
      case e:Exception =>
        log.error(s"failed to get hashes: '${json}'",e)
        Seq()
    }
  }

  def askBlocks(uri:BitcoinURI,blockHashes:Seq[String]):Seq[String] = {    
    val url = uri.url
    val blocksReq = blockHashes
      .map(blockHash => {
        s"""{
            "jsonrpc":"1.0","method":"getblock",
            "params":["${blockHash}",0],
            "id":0
          }""".trim.replaceAll("\\s+","")  
      })

    // if only 1 tx, don't batch (to be compatible with some weird RPC which don't support batch)
    val json = if(blockHashes.size == 1) 
      blocksReq.head 
    else 
      s"""[${blocksReq.mkString(",")}]"""
    
    try {
      log.debug(s"${json} -> ${url}")
      val rsp = requests.post(url, data = json,headers = Map("content-type" -> "application/json"))                        
      val body = rsp.text()
      
      rsp.statusCode match {
        case 200 => //
          log.trace(s"${body}")
        case _ => 
          // retry
          log.error(s"failed to get blocks: ${rsp.statusCode}: ${body}")
          throw new RetryException(s"${rsp.statusCode}")
      }
                      
      val batch = if(blockHashes.size == 1)
        decodeSingle(body)
      else
        decodeBatch(body)
      
      batch

    } catch {
      case e:Exception =>
        log.error(s"failed to get blocks: '${json}'",e)
        Seq()
    }
  }
    
}
