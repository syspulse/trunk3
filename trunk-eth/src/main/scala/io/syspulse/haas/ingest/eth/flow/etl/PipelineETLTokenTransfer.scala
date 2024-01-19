package io.syspulse.haas.ingest.eth.flow.etl

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
import io.syspulse.skel.ingest.flow.Flows

import spray.json._
import DefaultJsonProtocol._
import io.syspulse.skel.serde.Parq._
import com.github.mjakubowski84.parquet4s.{ParquetRecordEncoder,ParquetSchemaResolver}

import java.util.concurrent.TimeUnit

import io.syspulse.haas.ingest.eth._

import java.util.concurrent.atomic.AtomicLong

import io.syspulse.haas.ingest.eth.TokenTransferJson
import io.syspulse.haas.ingest.eth.TokenTransferJson._
import io.syspulse.haas.ingest.eth._
import io.syspulse.haas.ingest.Config
import io.syspulse.haas.ingest.eth.EthEtlJson._
import io.syspulse.haas.ingest.PipelineIngest

abstract class PipelineETLTokenTransfer[E <: skel.Ingestable](config:Config)
                                                             (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineIngest[EthTokenTransfer,TokenTransfer,E](config) with PipelineETL[E] {
  
  def apiSuffix():String = s"/transfer"

  def convert(tt:EthTokenTransfer):TokenTransfer = TokenTransfer(
    tt.blockTimestamp * 1000L,
    tt.blockNumber,
    tt.tokenAddress,
    tt.from,
    tt.to,
    tt.value,
    tt.txHash,
    tt.logIndex
  )
  
  def parse(data:String):Seq[EthTokenTransfer] = {
    val d = parseTokenTransfer(data)
    if(d.size!=0)
      latestTs.set(d.last.blockTimestamp * 1000L)
    d
  }

}

class PipelineTokenTransfer(config:Config) 
  extends PipelineETLTokenTransfer[TokenTransfer](config) {

  def transform(tt: TokenTransfer): Seq[TokenTransfer] = Seq(tt)
}
