package io.syspulse.haas.ingest.eth.flow.lake

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

import akka.util.ByteString
import akka.http.javadsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Flow

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

import io.syspulse.haas.core.Event
import io.syspulse.haas.serde.EventJson
import io.syspulse.haas.serde.EventJson._
import io.syspulse.haas.ingest.Config
import io.syspulse.haas.ingest.eth._
import io.syspulse.haas.ingest.eth.EthEtlJson._
import io.syspulse.haas.ingest.PipelineIngest

abstract class PipelineLakeEvent[E <: skel.Ingestable](config:Config)
                                                      (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineIngest[Event,Event,E](config) with PipelineLake[E] {
  
  def apiSuffix():String = s"/tx"

  def parse(data:String):Seq[Event] = {
    val d = parseEventLog(data)
    if(d.size!=0)
      latestTs.set(d.last.ts)
    d
  }

  def convert(tx:Event):Event = tx

}

class PipelineEvent(config:Config) 
  extends PipelineLakeEvent[Event](config) {

  def transform(tx: Event): Seq[Event] = Seq(tx)
}
