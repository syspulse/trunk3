package io.syspulse.haas.ingest

import java.util.concurrent.atomic.AtomicLong
import io.syspulse.skel.ingest.flow.Flows

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import java.util.concurrent.TimeUnit
import com.typesafe.scalalogging.Logger

import akka.util.ByteString
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Keep
import akka.stream.RestartSettings

import akka.http.scaladsl.model.{HttpRequest,HttpMethods,HttpEntity,ContentTypes}
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl

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

import io.syspulse.haas.ingest.Config
import io.jvm.uuid._

import io.syspulse.ext.core.ExtractorJson._

import io.syspulse.haas.intercept.Script
import io.syspulse.haas.intercept.ScriptInterceptor
import io.syspulse.haas.intercept.InterceptResult
import io.syspulse.blockchain.Blockchain

abstract class PipelineIngest[T,O <: skel.Ingestable,E <: skel.Ingestable](config:Config)
  (implicit val fmt:JsonFormat[E],
    parqEncoders0:ParquetRecordEncoder[E],parsResolver0:ParquetSchemaResolver[E]
  )
  extends PipelineIngestable[T,O,E,io.syspulse.ext.core.Events](config) {
  
  private val log = Logger(s"${this}")

  override val retrySettings:Option[RestartSettings] = Some(RestartSettings(
    minBackoff = FiniteDuration(1000,TimeUnit.MILLISECONDS),
    maxBackoff = FiniteDuration(1000,TimeUnit.MILLISECONDS),
    randomFactor = 0.2
  ))

  val blockchain = Blockchain(config.interceptorBlockchain)
  
  // interceptor can be changed in runtime
  @volatile
  var interceptor = if(config.script.isEmpty()) 
    None 
  else 
    Some(new ScriptInterceptor(Seq(Script("0",src = config.script,ts0 = System.currentTimeMillis))))

  // Dirty workaround to quick configure additiona callback for Default Interception Classes
  // configurable callback  
  @volatile
  var interceptCallaback: Option[(InterceptResult) => Unit] = None
  def setInterceptCallack(callback:(InterceptResult) => Unit) = {
    interceptCallaback = Some(callback)
  }

  def setInterceptor(id:String,script:String) = {
    interceptor = Some(new ScriptInterceptor(
      Seq(Script(id,src = script,ts0 = System.currentTimeMillis))
    ))
  }

  // default is Ext interceptor
  def interception(e:E) = interceptionExt(e)

  def interceptionExt(e:E):Seq[io.syspulse.ext.core.Events] = {
    interceptor match {
      case Some(interceptor) => 
        
        // run intercept    
        val r = interceptor.scan[E](e)
        
        if(interceptCallaback.isDefined) {
          r.foreach{ r => 
            // call callback
            (interceptCallaback.get)(r) 
        }}

        r.map(r => { 
          
          val event = io.syspulse.ext.core.Event(
            did = config.interceptorName,
            eid = UUID.random.toString,
            sid = config.interceptorSid,
            category = config.interceptorCat,
            `type` = config.interceptorType,
            severity = config.interceptorSeverity,
            ts = System.currentTimeMillis(),
            blockchain = io.syspulse.ext.core.Blockchain(blockchain.id.getOrElse(""),blockchain.name),
            metadata = Map(
              "tx_hash" -> r.txHash,
              "monitored_contract" -> config.interceptorContract,
            ) ++ r.data
          )

          io.syspulse.ext.core.Events(events = Seq(event))
        })
      
      case None => 
        Seq()
    }
  }

  // Additional sink where data is piped
  // override def sink0() = {    
  //   import io.syspulse.ext.core.ExtractorJson._
  //   import io.syspulse.ext.core.Blockchain
        
  //   val f = Flow[E].mapConcat( e => {
  //     interception[io.syspulse.ext.core.Events](e)
  //   })

  //   val s0 = sinking[io.syspulse.ext.core.Events](config.alertOutput)
  //   f.to(s0)    
  // }

}
