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

import io.syspulse.haas.ingest.IngestUtil

import io.syspulse.haas.ingest.eth._
import io.syspulse.haas.ingest.eth.TransactionJson
import io.syspulse.haas.ingest.eth.TransactionJson._
import io.syspulse.haas.ingest.eth.BlockJson
import io.syspulse.haas.ingest.eth.BlockJson._

import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson._
import io.syspulse.haas.ingest.PipelineIngest


class PipelineWsBlockTransaction(config:Config) extends PipelineWsHead[Block](config) {
    
  def transform(b: Block): Seq[Block] = {
    val txs = decodeBlock(b)
    Seq(
      b.copy(tx = Some(txs.toArray),txn = txs.size)
    )
  }
}

class PipelineWsTransaction(config:Config) extends PipelineWsHead[Transaction](config) {

  def transform(b: Block): Seq[Transaction] = {
    decodeBlock(b)
  }
}
