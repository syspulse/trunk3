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
import io.syspulse.haas.ingest.eth.TxJson
import io.syspulse.haas.ingest.eth.TxJson._
import io.syspulse.haas.ingest.Config
import io.syspulse.haas.ingest.PipelineIngest

abstract class PipelineLakeTx[E <: skel.Ingestable](config:Config)(implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineIngest[Tx,Tx,E](config) with PipelineLake[E] {
  
  def apiSuffix():String = s"/tx"

  def parse(data:String):Seq[Tx] = {
    val d = parseTx(data)
    if(d.size!=0)
      latestTs.set(d.last.block.ts)
    d
  }

  def convert(tx:Tx):Tx = tx

}

class PipelineTx(config:Config) 
  extends PipelineLakeTx[Tx](config) {

  def transform(tx: Tx): Seq[Tx] = Seq(tx)
}
