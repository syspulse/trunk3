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

import io.syspulse.haas.ingest.eth._

import io.syspulse.haas.ingest.eth.Transaction
import io.syspulse.haas.ingest.eth.TransactionJson._

import io.syspulse.haas.ingest.Config
import io.syspulse.haas.ingest.eth.flow.rpc3._
import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson

abstract class PipelineRpcTransaction[E <: skel.Ingestable](config:Config)
                (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineRPC[RpcBlock,RpcBlock,E](config) {
  
  def apiSuffix():String = s"/transaction"

  def parse(data:String):Seq[RpcBlock] = {
    val bb = parseBlock(data)
    if(bb.size!=0) {
      val b = bb.last.result.get
      latestTs.set(toLong(b.timestamp) * 1000L)
    }    
    bb
  }

  def convert(block:RpcBlock):RpcBlock = {
    block
  }
}

class PipelineTransaction(config:Config) extends PipelineRpcTransaction[Transaction](config) {
  import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson._
  
  def transform(block: RpcBlock): Seq[Transaction] = {
    val b = block.result.get

    val ts = toLong(b.timestamp)
    val block_number = toLong(b.number)

    log.info(s"Block(${block_number},${b.transactions.size})")
      
    val receipts:Map[String,RpcReceipt] = decodeReceipts(block)
    
    val txx = b.transactions.map{ tx:RpcTx => {
      val transaction_index = toLong(tx.transactionIndex).toInt
      val receipt = receipts.get(tx.hash)
      
      Transaction(
        ts * 1000L,
        transaction_index,
        tx.hash,
        block_number,

        tx.from,
        tx.to,
        
        toLong(tx.gas),
        toBigInt(tx.gasPrice),
        
        tx.input,
        toBigInt(tx.value),
        toLong(tx.nonce),
        
        tx.maxFeePerGas.map(toBigInt(_)), //tx.max_fee_per_gas,
        tx.maxPriorityFeePerGas.map(toBigInt(_)), //tx.max_priority_fee_per_gas, 

        tx.`type`.map(r => toLong(r).toInt),

        receipt.map(r => toLong(r.cumulativeGasUsed)).getOrElse(0L), //0L,//tx.receipt_cumulative_gas_used, 
        receipt.map(r => toLong(r.gasUsed)).getOrElse(0L), //0L,//tx.receipt_gas_used, 
        receipt.map(_.contractAddress).flatten, //tx.receipt_contract_address, 
        Some(b.receiptsRoot), //tx.receipt_root, 
        receipt.flatMap(r => r.status.map(toLong(_).toInt)), //tx.receipt_status, 
        receipt.map(_.effectiveGasPrice.map(r => toBigInt(r))).flatten, //tx.receipt_effective_gas_price

      )
    }}.toSeq

    if(receipts.size == b.transactions.size) {
      // commit cursor only if all transactions receipts recevied !
      cursor.commit(block_number)
    }

    txx
  }
}
