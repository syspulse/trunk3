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

import io.syspulse.skel.config._
import io.syspulse.skel.util.Util
import io.syspulse.skel.util.DiffSet
import io.syspulse.skel.config._

import io.syspulse.skel.Ingestable
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
import akka.stream.Attributes

abstract class PipelineFlowWS[T,O <: Ingestable,E <: Ingestable](config:Config)
  (implicit fmt:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E])
  extends PipelineIngest[T,O,E](config.copy(throttle = 0L))(fmt,parqEncoders,parsResolver) with RPCDecoder[E] {

  val delta = true

  import EthRpcJson._
  
  implicit val uri = EthURI(config.feed,config.apiToken)

  val reorg = config.reorgFlow match {
    case "reorg1" => new ReorgBlock1(config.blockReorg)
    case "reorg2" => new ReorgBlock2(config.blockReorg)
    case _ => new ReorgBlock2(config.blockReorg)
  }
    
  override def source(feed:String) = {
    feed.split("://").toList match {
      case "ws" :: _ | "wss" :: _  =>         
        
        val s0 = fromWebsocket(
          uri.uri, 
          helloMsg = Some("""{"jsonrpc":"2.0","id":1,"method":"eth_subscribe","params":["newHeads"]}""")
        )
        
        // ----- Reorg Subflow ------------------------------------------------------------------------
        val reorgFlow = (lastBlock:String) => {
          if(config.blockReorg > 0 ) { 
            val (fresh,_) = reorg.track(lastBlock)
            
            if(! fresh) {
              log.error(s"reorg: fresh=${fresh}, ${lastBlock}")
            }
            
            fresh

          } else true
        }

        val sourceFlow = s0
          .throttle(1,FiniteDuration(config.throttle,TimeUnit.MILLISECONDS))
          .drop(1)  // drop subscription response message
          .filter(b => { 
            // fitler reconnect messages
            try {
              val result = ujson.read(b.utf8String).obj("params").obj("result").obj
              val blockNum = java.lang.Long.decode(result("number").str).toLong
              val blockHash = result("hash").str

              // emulate PipelineRPC
              log.info(s"--> Vector(${blockNum})")
              true

            } catch {
              case e:Exception => 
                log.warn(s"failed to parse block (reconnect): '${b.utf8String}'",e)
                false
            }
                        
          })
          .filter( b => 
            // it must return True to continue processing or false (when duplicated due to reorg algo)
            reorgFlow(b.utf8String)
          )
          .map(b => ByteString(b))
      
        sourceFlow

      case _ => super.source(feed)
    }
  }
}
