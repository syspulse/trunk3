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
import io.syspulse.skel.blockchain.eth.EthUtil

abstract class PipelineRpcTransaction[E <: skel.Ingestable](config:Config)
                (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineRPC[RpcBlock,RpcBlock,E](config) {
  
  def apiSuffix():String = s"/transaction"

  def parse(data:String):Seq[RpcBlock] = {
    val bb = parseBlock(data)
    if(bb.size!=0) {
      val b = bb.last.result.get
      latestTs.set(EthUtil.toLong(b.timestamp) * 1000L)
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
  //   val b = block.result.get

  //   val ts = EthUtil.toLong(b.timestamp)
  //   val block_number = EthUtil.toLong(b.number)

  //   log.info(s"Block(${block_number},${b.transactions.size})")
      
  //   val receipts:Map[String,RpcReceipt] = decodeReceipts(block)
    
  //   val txx = b.transactions
  //   .filter(tx => {
  //     config.filter.size == 0 || config.filter.contains(tx.hash)
  //   })
  //   .map{ tx:RpcTx => {
  //     val transaction_index = EthUtil.toLong(tx.transactionIndex).toInt
  //     val logs = receipts.get(tx.hash).get.logs
  //     val receipt = receipts.get(tx.hash)
      
  //     Transaction(
  //       ts * 1000L,
  //       transaction_index,
  //       tx.hash,
  //       block_number,

  //       formatAddr(tx.from,config.formatAddr),
  //       formatAddr(tx.to,config.formatAddr),
        
  //       EthUtil.toLong(tx.gas),
  //       EthUtil.toBigInt(tx.gasPrice),
  //       tx.input,
  //       EthUtil.toBigInt(tx.value),

  //       EthUtil.toBigInt(tx.nonce),
        
  //       tx.maxFeePerGas.map(EthUtil.toBigInt(_)), //tx.max_fee_per_gas,
  //       tx.maxPriorityFeePerGas.map(EthUtil.toBigInt(_)), //tx.max_priority_fee_per_gas, 

  //       tx.`type`.map(r => EthUtil.toLong(r).toInt),

  //       receipt.map(r => EthUtil.toLong(r.cumulativeGasUsed)).getOrElse(0L), //0L,//tx.receipt_cumulative_gas_used, 
  //       receipt.map(r => EthUtil.toLong(r.gasUsed)).getOrElse(0L), //0L,//tx.receipt_gas_used, 
  //       receipt.map(r => formatAddr(r.contractAddress,config.formatAddr)).flatten, //tx.receipt_contract_address, 
  //       Some(b.receiptsRoot), //tx.receipt_root, 
  //       receipt.flatMap(r => r.status.map(EthUtil.toLong(_).toInt)), //tx.receipt_status, 
  //       receipt.map(_.effectiveGasPrice.map(r => EthUtil.toBigInt(r))).flatten, //tx.receipt_effective_gas_price

  //       logs = Some(logs.map( r => {
  //         EventTx(
  //           EthUtil.toLong(r.logIndex).toInt,
  //           formatAddr(r.address,config.formatAddr),
  //           r.data,
  //           r.topics
  //         )
  //       }))
  //     )
  //   }}.toSeq

  //   if(receipts.size == b.transactions.size) {
  //     // commit cursor only if all transactions receipts recevied !
  //     cursor.commit(block_number)
  //   }

  //   txx  
    
    val (txx,receipts) = decodeTransactions(block)(config,uri.uri)

    if(receipts.size == block.result.get.transactions.size) {
      // commit cursor only if all transactions receipts recevied !
      cursor.commit(EthUtil.toLong(block.result.get.number))
    }

    txx
  }
}
