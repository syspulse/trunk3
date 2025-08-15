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

import io.syspulse.haas.ingest.eth._
import io.syspulse.haas.ingest.eth.TxJson
import io.syspulse.haas.ingest.eth.TxJson._
import io.syspulse.haas.ingest.Config
import io.syspulse.haas.ingest.eth.EthEtlJson._
import io.syspulse.haas.ingest.PipelineIngest

abstract class PipelineETLTx[E <: skel.Ingestable](config:Config)
                                                  (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineIngest[EthTx,Tx,E](config) with PipelineETL[E] {
  
  def apiSuffix():String = s"/tx"

  def parse(data:String):Seq[EthTx] = {
    val d = parseTx(data)
    if(d.size!=0)
      latestTs.set(d.last.block.timestamp * 1000L)
    d
  }

  def convert(tx:EthTx):Tx = Tx(
    tx.transaction_index,
    tx.hash,
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
    tx.cumulative_gas_used, 
    tx.gas_used, 
    tx.contract_address, 
    tx.root, 
    tx.status, 
    tx.effective_gas_price,

    block = Block(
      tx.block.number,
      tx.block.hash,
      
      tx.block.parent_hash,
      Option(tx.block.nonce),
      tx.block.sha3_uncles,
      tx.block.logs_bloom,
      tx.block.transactions_root,
      tx.block.state_root,
      tx.block.receipts_root,
      tx.block.miner,
      tx.block.difficulty,
      tx.block.total_difficulty,
      tx.block.size,
      tx.block.extra_data, 
      tx.block.gas_limit, 
      tx.block.gas_used, 
      tx.block.timestamp * 1000L, 

      tx.block.transaction_count,
      tx.block.base_fee_per_gas,
    ),
    
    logs = tx.logs.map(e => EventTx(
      e.index,
      e.address, 
      e.data,
      e.topics,
    )),

    sim = config.sim
  )

}

class PipelineTx(config:Config) 
  extends PipelineETLTx[Tx](config) {

  def transform(tx: Tx): Seq[Tx] = Seq(tx)
}
