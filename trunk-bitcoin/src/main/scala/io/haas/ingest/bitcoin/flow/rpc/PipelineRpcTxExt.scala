package io.haas.ingest.bitcoin.flow.rpc

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import scala.util.{Try,Success,Failure}
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

import io.haas.ingest.bitcoin.flow.rpc.RpcBlock
import io.haas.ingest.bitcoin.flow.rpc.RpcJsonProtocol._
import io.haas.ingest.bitcoin.{Tx, Block}
import io.haas.ingest.ext.{TxExt, BlockExt, LogExt}
import io.haas.ingest.ext.IngestExtJson._
import io.haas.ingest.bitcoin.BitcoinJson._

import java.util.concurrent.TimeUnit

import io.haas.ingest.Config

class PipelineTxExt(config:Config) extends PipelineRpcTx[TxExt](config) {

  def transform(block: RpcBlock): Seq[TxExt] = {
    
    transformTx(block).map(tx => {
      // Convert bitcoin Block to BlockExt
      val blockExt = BlockExt(
        number = tx.block.i,
        hash = tx.block.hash,
        parent_hash = tx.block.phash,
        nonce = Some(tx.block.nonce.toString),
        sha3_uncles = None,
        logs_bloom = "",
        transactions_root = tx.block.merkle,
        state_root = "",
        receipts_root = "",
        miner = "",
        difficulty = tx.block.d,
        total_difficulty = None,
        size = tx.block.sz.toLong,
        extra_data = "",
        gas_limit = 0L,
        gas_used = 0L,
        timestamp = tx.block.ts,
        transaction_count = tx.block.n.toLong,
        base_fee_per_gas = None
      )
      
      TxExt(
        hash = tx.hash,
        nonce = BigInt(0), // Bitcoin doesn't have nonce in the same way
        transaction_index = tx.i.map(_.toInt).getOrElse(0),
        from_address = tx.from,
        to_address = Some(tx.to),
        value = tx.v,
        gas = 0L, // Bitcoin doesn't have gas
        gas_price = None, // Bitcoin doesn't have gas price
        input = "", // Bitcoin doesn't have input data in the same way
        max_fee_per_gas = None,
        max_priority_fee_per_gas = None,
        transaction_type = None,
        receipt_cumulative_gas_used = 0L,
        receipt_gas_used = 0L,
        receipt_contract_address = None,
        receipt_root = None,
        receipt_status = None,
        receipt_effective_gas_price = None,
        block = blockExt,
        logs = Array.empty[LogExt], // Bitcoin doesn't have logs
        sim = None
      )
    })
  }
}
