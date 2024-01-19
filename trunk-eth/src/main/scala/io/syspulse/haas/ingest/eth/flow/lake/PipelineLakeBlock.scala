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

import io.syspulse.haas.ingest.eth._
import io.syspulse.haas.ingest.eth.BlockJson
import io.syspulse.haas.ingest.eth.BlockJson._
import io.syspulse.haas.ingest.Config
import io.syspulse.haas.ingest.PipelineIngest

abstract class PipelineLakeBlock[E <: skel.Ingestable](config:Config)
                                                      (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineIngest[Block,Block,E](config) with PipelineLake[E] {
  
  def apiSuffix():String = s"/block"

  def parse(data:String):Seq[Block] = {
    val d = parseBlock(data)
    if(d.size!=0)
      latestTs.set(d.last.ts)
    d
  }

  def convert(tx:Block):Block = tx

}

class PipelineBlock(config:Config) 
  extends PipelineLakeBlock[Block](config) {

  def transform(b: Block): Seq[Block] = Seq(b)
}
