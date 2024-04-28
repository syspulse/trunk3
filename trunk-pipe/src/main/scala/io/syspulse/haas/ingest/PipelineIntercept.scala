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

import io.syspulse.haas.intercept.Script
import io.syspulse.haas.intercept.ScriptInterceptor

case class InterceptOutput(data:Map[String,String]) extends skel.Ingestable

object InterceptOutputJson extends DefaultJsonProtocol {  
  implicit val jf_inx_output = jsonFormat1(InterceptOutput)  
}

import InterceptOutputJson._


abstract class PipelineIntercept[T,O <: skel.Ingestable,E <: skel.Ingestable]
  (config:Config)
  (implicit val fmt10:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E])
  extends PipelineIngestable[T,O,E,InterceptOutput](config) {
  
  private val log = Logger(s"${this}")

  var interceptor = if(config.script.isEmpty()) 
    None 
  else 
    Some(new ScriptInterceptor(Seq(Script("0",src = config.script,ts0 = System.currentTimeMillis))))

  def onIntercept(io: InterceptOutput):Unit
    
  def interception(e:E):Seq[InterceptOutput] = {
    interceptor match {
      case Some(interceptor) => 
        
        // run intercept    
        val r = interceptor.scan[E](e)

        r.map(r => {
          InterceptOutput(data = r.data)
        })
      
      case None => 
        Seq()
    }
  }

}
