package io.syspulse.haas.ingest.eth.flow.rpc

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

import io.syspulse.haas.ingest.eth._
import io.syspulse.haas.ingest.eth.rpc._
import io.syspulse.haas.ingest.eth.rpc.EthRpcJson._

import io.syspulse.haas.ingest.eth.EthURI
import io.syspulse.haas.ingest.PipelineIngest
import io.syspulse.haas.ingest.eth

import io.syspulse.haas.ingest.Config
import akka.actor.typed.ActorSystem
import akka.stream.RestartSettings
import scala.util.control.NoStackTrace

class RetryException(msg: String) extends RuntimeException(msg) with NoStackTrace
class BehindException(behind: Long) extends RuntimeException("") with NoStackTrace

abstract class PipelineRPC[T,O <: skel.Ingestable,E <: skel.Ingestable](config:Config)
                                                                       (implicit fmt:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E])
  extends PipelineIngest[T,O,E](config)(fmt,parqEncoders,parsResolver) with RPCDecoder[E] {

  override val retrySettings:Option[RestartSettings] = Some(RestartSettings(
    minBackoff = FiniteDuration(1000,TimeUnit.MILLISECONDS),
    maxBackoff = FiniteDuration(1000,TimeUnit.MILLISECONDS),
    randomFactor = 0.2
  ))

  import EthRpcJson._

  val lastBlock = new LastBlock()
    
  override def source(feed:String) = {
    feed.split("://").toList match {
      case "http" :: _ | "https" :: _ => {
        if( ! lastBlock.isDefined) {
          val (blockStr,stateFile) = 
            (config.block.split("://").toList match {
              case "file" :: file :: Nil => // state file
                (os.read(os.Path(file,os.pwd)),Some(file))
              case _ => 
                (config.block,None)
            })

          val blockStart = blockStr.strip match {
            case "latest" =>
              val rsp = requests.post(feed,
                headers = Seq(("Content-Type","application/json")),
                data = s"""{
                  "jsonrpc":"2.0","method":"eth_getBlockByNumber",
                  "params":["latest",false],
                  "id":0
                }""".trim.replaceAll("\\s+","")
              )
              if(rsp.statusCode != 200) {
                log.error(s"failed to get latest block: ${rsp}")
                0
              } else {
                val latest = ujson.read(rsp.text()).obj("result").obj("number").str
                java.lang.Long.parseLong(latest.stripPrefix("0x"),16).toInt
              }
            case hex if hex.startsWith("0x") =>
              java.lang.Long.parseLong(hex,16).toInt
            case dec =>
              dec.toInt
          }
          
          val blockEnd = config.blockEnd match {
            case "" => Int.MaxValue
            case "latest" => blockStart
            case hex if hex.startsWith("0x") =>
              java.lang.Long.parseLong(hex,16).toInt
            case _ @ dec =>
              dec.toInt
          }

          lastBlock.set(next = blockStart, blockStart, blockEnd, stateFile, config.blockLag)
          
        } else {
          
        }

        log.info(s"blocks: ${LastBlock}")                    
        //log.info(s"reqs=${reqs}")

        val sourceHttp = Source.tick(FiniteDuration(10,TimeUnit.MILLISECONDS), 
                            FiniteDuration(config.ingestCron.toLong,TimeUnit.SECONDS),
                            s"${feed}")
        .map(h => {
          log.info(s"Cron --> ${h}")          

          lazy val reqs = LazyList.from(lastBlock.next().toInt,1).takeWhile(_ <= lastBlock.end()).map { block => 
            val id = System.currentTimeMillis() / 1000L
            val blockHex = "latest"// "0x%x".format(block) //
            val json = s"""{
                "jsonrpc":"2.0","method":"eth_getBlockByNumber",
                "params":["${blockHex}",true],
                "id":${block}
              }""".trim.replaceAll("\\s+","")
            
            log.info(s"block=${block}: req='${json}'")
              
            HttpRequest( method = HttpMethods.POST, uri = feed,
                entity = HttpEntity(ContentTypes.`application/json`,json)
              ).withHeaders(Accept(MediaTypes.`application/json`))
          }
          reqs
        })
        .mapConcat(reqs => {
          reqs
        })
        .throttle(1,FiniteDuration(config.throttle,TimeUnit.MILLISECONDS))
        .flatMapConcat(req => {
          log.info(s"--> ${req}")
          
          Flows.fromHttpRestartable(req, config.delimiter, config.buffer, retrySettings.get)
        })          
        sourceHttp          
      }
            
      case _ => super.source(feed)
    }
  }
}
