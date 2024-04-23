package io.syspulse.haas.ingest.eth.flow.rpc3

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

import io.syspulse.haas.ingest.Config
import io.syspulse.haas.ingest.eth._

import io.syspulse.haas.ingest.eth._
import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson._
import io.syspulse.haas.ingest.eth.MempoolJson._
import io.syspulse.haas.ingest.eth.CallTraceJson._
import io.syspulse.haas.ingest.PipelineIngest
import io.syspulse.haas.ingest.eth.flow.rpc3.RpcTxPoolResult

abstract class PipelineRpcMempoolTrace[E <: skel.Ingestable](config:Config)
                                                       (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineMempoolRPC[MempoolTx,MempoolTx,E](config) {
  
  def apiSuffix():String = s"/trace"

  // only json is supported !
  override def parse(data:String):Seq[MempoolTx] = {
    val pool = parseMempool(data,true)
    pool
  }

  def convert(mtx: MempoolTx): MempoolTx = mtx
}

class PipelineMempoolTrace(config:Config) extends PipelineRpcMempoolTrace[CallTrace](config) {

  def transform(mtx: MempoolTx): Seq[CallTrace] = {
    traceMempoolTx(mtx.hash)(config)    
  }
}
