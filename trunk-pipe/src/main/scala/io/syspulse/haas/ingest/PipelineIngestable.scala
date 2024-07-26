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

abstract class PipelineIngestable[T,O <: skel.Ingestable,E <: skel.Ingestable,W <: skel.Ingestable](config:Config)
  (implicit val fmt0:JsonFormat[E],fmt1:JsonFormat[W],
    parqEncoders0:ParquetRecordEncoder[E],parsResolver0:ParquetSchemaResolver[E],
    parqEncoders1:ParquetRecordEncoder[W],parsResolver1:ParquetSchemaResolver[W]
  )
  extends Pipeline[T,O,E](config.feed,config.output,config.throttle,config.delimiter,config.buffer,format=config.format) {
  
  private val log = Logger(s"${this}")
  
  var latestTs:AtomicLong = new AtomicLong(0)
  
  override def getRotator():Flows.Rotator = 
    new Flows.RotatorTimestamp(() => {
      latestTs.get()
    })

  override def getFileLimit():Long = config.limit
  override def getFileSize():Long = config.size

  //def filter():Seq[String] = config.filter
  def apiSuffix():String

  def convert(t:T):O

  //def transform(o: O) = Seq(o)

  def process:Flow[T,O,_] = Flow[T].map(t => {
    val o = convert(t)
    o
  })

  def interception(e:E):Seq[W]
  
  // Additional sink where data is piped
  override def sink0() = {          
    val f = Flow[E].mapConcat( e => {
      interception(e)
    })

    val s0 = sinking[W](config.alertOutput)
    f.to(s0)    
  }

}
