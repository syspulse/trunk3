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
import akka.stream.scaladsl.RestartSource

import io.syspulse.skel.ingest.flow.AttributeActor

import io.syspulse.haas.core.RetryException
import akka.stream.Attributes

abstract class PipelineMempoolWS[T,O <: Ingestable,E <: Ingestable](config:Config)
                                                                    (implicit fmt:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E])
  extends PipelineIngest[T,O,E](config.copy(throttle = 0L))(fmt,parqEncoders,parsResolver) with RPCDecoder[E] {

  val delta = true

  override val retrySettings:Option[RestartSettings] = Some(RestartSettings(
    minBackoff = FiniteDuration(1000,TimeUnit.MILLISECONDS),
    maxBackoff = FiniteDuration(1000,TimeUnit.MILLISECONDS),
    randomFactor = 0.2
  ))

  import EthRpcJson._

  implicit val uri = EthURI(config.feed,config.apiToken)
    
  override def source(feed:String) = {
    feed.split("://").toList match {
      case "ws" :: _ | "wss" :: _  =>         
        
        val s0 = Flows.fromWebsocket(uri.uri, 
          helloMsg = Some("""{"jsonrpc":"2.0","id":1,"method":"eth_subscribe","params":["newPendingTransactions"]}""")
        )

        // val a = s0.getAttributes.getAttribute(classOf[AttributeActor])
        // log.info(s"Websocket Actor: ${a}")        

        // ------- Flow ------------------------------------------------------------------------------------
        val sourceFlow = s0
          .map(b => ByteString(b))
      
        // val sourceRestart = RestartSource.onFailuresWithBackoff(retrySettings.get) { () =>
        //   log.info(s"connect -> ${uri.uri}")
        //   sourceFlow
        // }

        // sourceRestart
        sourceFlow

      case _ => super.source(feed)
    }
  }
  
  
}
