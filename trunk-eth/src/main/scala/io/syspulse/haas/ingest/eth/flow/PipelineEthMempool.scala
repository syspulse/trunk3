package io.syspulse.haas.ingest.eth.flow

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

import io.syspulse.haas.ingest.eth.EvmTx
import io.syspulse.haas.core.MempoolTx
import io.syspulse.haas.ingest.eth.EvmTxPoolJson._
import io.syspulse.haas.serde.MempoolJson._
import io.syspulse.haas.ingest.PipelineIngest

abstract class PipelineEthMempool[E <: skel.Ingestable](config:Config)
                                                       (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineIngest[EvmTx,MempoolTx,E](config) {
  
  def apiSuffix():String = s"/mempool"

  // only json is supported !
  override def parse(data:String):Seq[EvmTx] = Seq(
    data.parseJson.convertTo[EvmTx]
  )

  def convert(etx: EvmTx): MempoolTx = 
    MempoolTx(
      ts = etx.ts,
      pool = (if(etx.pool == "pending") 0 else 1),
      from = etx.from,
      gas = etx.gas,
      p = etx.gasPrice,
      fee = etx.maxFeePerGas,
      tip = etx.maxPriorityFeePerGas,
      hash = etx.hash,
      inp = etx.input,
      non = etx.nonce,
      to = etx.to,
      v = etx.value,
      typ = etx.`type`,
      sig = s"${etx.r}:${etx.s}:${etx.v}"
    )

}

class PipelineMempool(config:Config)
  extends PipelineEthMempool[MempoolTx](config) {

  def transform(tx: MempoolTx): Seq[MempoolTx] = Seq(tx)    
}
