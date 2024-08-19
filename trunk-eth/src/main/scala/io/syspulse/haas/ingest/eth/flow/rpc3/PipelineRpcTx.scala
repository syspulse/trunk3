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

import io.syspulse.haas.ingest.eth.Tx
import io.syspulse.haas.ingest.eth.TxJson._

import io.syspulse.haas.ingest.Config
import io.syspulse.haas.ingest.eth.flow.rpc3._
import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson
import io.syspulse.haas.ingest.IngestUtil

abstract class PipelineRpcTx[E <: skel.Ingestable](config:Config)
                                                  (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineRPC[RpcBlock,RpcBlock,E](config) {
  
  def apiSuffix():String = s"/tx"

  def parse(data:String):Seq[RpcBlock] = {
    val bb = parseBlock(data)
    if(bb.size!=0) {
      val b = bb.last.result.get
      latestTs.set(IngestUtil.toLong(b.timestamp) * 1000L)
    }
    
    bb
  }

  def convert(block:RpcBlock):RpcBlock = {
    block
  }
}

class PipelineTx(config:Config) extends PipelineRpcTx[Tx](config) {
  import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson._
  
  def transform(blk: RpcBlock): Seq[Tx] = {
    val b = blk.result.get

    val ts = IngestUtil.toLong(b.timestamp)
    val block_number = IngestUtil.toLong(b.number)

    log.info(s"Block[${block_number},${b.transactions.size}]")
      
    val receipts:Map[String,RpcReceipt] = decodeReceipts(blk)

    val block = Block(
      IngestUtil.toLong(b.number),
      b.hash,
      b.parentHash,
      b.nonce,
      b.sha3Uncles,        
      b.logsBloom,
      b.transactionsRoot,
      b.stateRoot,        
      b.receiptsRoot,
      formatAddr(b.miner),
      
      IngestUtil.toBigInt(b.difficulty),
      IngestUtil.toBigInt(b.totalDifficulty),
      IngestUtil.toLong(b.size),

      b.extraData, 
          
      IngestUtil.toLong(b.gasLimit), 
      IngestUtil.toLong(b.gasUsed), 
      IngestUtil.toLong(b.timestamp) * 1000L, 
      b.transactions.size,
      b.baseFeePerGas.map(d => IngestUtil.toLong(d))
    )

    val txx = b.transactions
    .filter(tx => {
      config.filter.size == 0 || config.filter.contains(tx.hash)
    })
    .map{ tx:RpcTx => {
      val transaction_index = IngestUtil.toLong(tx.transactionIndex).toInt
      val logs = receipts.get(tx.hash).get.logs
      val receipt = receipts.get(tx.hash)

      Tx(
        // ts * 1000L,
        transaction_index,
        tx.hash,
        // block_number,

        formatAddr(tx.from),
        formatAddr(tx.to),
        
        IngestUtil.toLong(tx.gas),
        IngestUtil.toBigInt(tx.gasPrice),
        
        tx.input,
        IngestUtil.toBigInt(tx.value),
        IngestUtil.toLong(tx.nonce),
        
        tx.maxFeePerGas.map(IngestUtil.toBigInt(_)), //tx.max_fee_per_gas,
        tx.maxPriorityFeePerGas.map(IngestUtil.toBigInt(_)), //tx.max_priority_fee_per_gas, 

        tx.`type`.map(r => IngestUtil.toLong(r).toInt),

        receipt.map(r => IngestUtil.toLong(r.cumulativeGasUsed)).getOrElse(0L), //0L,//tx.receipt_cumulative_gas_used, 
        receipt.map(r => IngestUtil.toLong(r.gasUsed)).getOrElse(0L), //0L,//tx.receipt_gas_used, 
        receipt.map(r => formatAddr(r.contractAddress)).flatten, //tx.receipt_contract_address, 
        Some(b.receiptsRoot), //tx.receipt_root, 
        receipt.flatMap(r => r.status.map(IngestUtil.toLong(_).toInt)), //tx.receipt_status, 
        receipt.map(_.effectiveGasPrice.map(r => IngestUtil.toBigInt(r))).flatten, //tx.receipt_effective_gas_price

        block = block,

        logs = logs.map( r => {
          EventTx(
            IngestUtil.toLong(r.logIndex).toInt,
            formatAddr(r.address),
            r.data,
            r.topics
          )
        })
      )
    }}.toSeq

    if(receipts.size == b.transactions.size) {
      // commit cursor only if all transactions receipts recevied !
      cursor.commit(block_number)
    }

    txx
  }
}
