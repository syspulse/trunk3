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

import spray.json._
import DefaultJsonProtocol._
import io.syspulse.skel.serde.Parq._
import com.github.mjakubowski84.parquet4s.{ParquetRecordEncoder,ParquetSchemaResolver}

import java.util.concurrent.TimeUnit

import io.syspulse.haas.core.Transaction
import io.syspulse.haas.serde.TransactionJson
import io.syspulse.haas.serde.TransactionJson._
import io.syspulse.haas.ingest.eth._
import io.syspulse.haas.ingest.Config
import io.syspulse.haas.ingest.eth.EthEtlJson._
import io.syspulse.haas.ingest.PipelineIngest

abstract class PipelineETLTransaction[E <: skel.Ingestable](config:Config)
                                                  (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineIngest[EthTransaction,Transaction,E](config) with PipelineETL[E] {
  
  def apiSuffix():String = s"/transaction"

  def parse(data:String):Seq[EthTransaction] = {
    val d = parseTransaction(data)
    if(d.size!=0)
      latestTs.set(d.last.block_timestamp * 1000L)
    d
  }

  def convert(tx:EthTransaction):Transaction = Transaction(
    tx.block_timestamp * 1000L,
    tx.transaction_index,
    tx.hash,
    tx.block_number,
    tx.from_address,
    tx.to_address,
    tx.gas,
    tx.gas_price,
    tx.input,
    tx.value,

    tx.nonce,
    tx.max_fee_per_gas,
    tx.max_priority_fee_per_gas, 
    tx.transaction_type, 
    tx.receipt_cumulative_gas_used, 
    tx.receipt_gas_used, 
    tx.receipt_contract_address, 
    tx.receipt_root, 
    tx.receipt_status, 
    tx.receipt_effective_gas_price
  )

}

class PipelineTransaction(config:Config) 
  extends PipelineETLTransaction[Transaction](config) {

  def transform(tx: Transaction): Seq[Transaction] = Seq(tx)
}
