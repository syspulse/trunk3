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

import io.syspulse.haas.ingest.eth.etl.Tx
import io.syspulse.haas.ingest.eth.etl.Block
import io.syspulse.haas.ingest.eth.etl.LogTx
import io.syspulse.haas.ingest.eth.etl.TxJson._

import io.syspulse.haas.ingest.eth.flow.rpc3._
import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson
import io.syspulse.haas.ingest.IngestUtil

// ====================================================================================================
// ATTENTION !
// 
// This is a special Pipeline to be compatible with legacy ethereum-etl output fot tx (Fat Transaction)
// ====================================================================================================
abstract class PipelineRpcTxETL[E <: skel.Ingestable](config:Config)
                                                  (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineRPC[RpcBlock,RpcBlock,E](config) {
  
  def apiSuffix():String = s"/tx.etl"

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

class PipelineTxETL(config:Config) extends PipelineRpcTxETL[Tx](config) {
  import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson._
  
  def transform(blk: RpcBlock): Seq[Tx] = {
    val b = blk.result.get

    val ts = IngestUtil.toLong(b.timestamp)
    val block_number = IngestUtil.toLong(b.number)
         
    val receipts:Map[String,RpcReceipt] = decodeReceipts(blk)
    
    val numEvents = receipts.values.foldLeft(0)((c,r) => c + r.logs.size)
    val numTransfers = b.transactions.foldLeft(0)((c,t) => c + {if(t.input.isEmpty() || t.input == "0x") 0 else 1})
    val numCalls = b.transactions.size - numTransfers
    log.info(s"Block[${block_number},${b.transactions.size},${receipts.size},${numEvents},${numTransfers},${numCalls},${IngestUtil.toLong(b.size)}]")

    if(receipts.size != b.transactions.size) {
      log.error(s"block=${block_number}: transactions=${b.transactions.size} != receipts=${receipts.size}")
      return Seq()
    }

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
      formatAddr(b.miner,config.formatAddr),
      
      IngestUtil.toBigInt(b.difficulty),
      IngestUtil.toBigInt(b.totalDifficulty),
      IngestUtil.toLong(b.size),

      b.extraData, 
          
      IngestUtil.toLong(b.gasLimit), 
      IngestUtil.toLong(b.gasUsed), 
      IngestUtil.toLong(b.timestamp) * 1000L,  // ATTENTION: ETL compatibility is broken here !!!
      b.transactions.size,
      b.baseFeePerGas.map(d => IngestUtil.toLong(d))
    )

    val txx = b.transactions
    .filter(tx => {
      config.filter.size == 0 || config.filter.contains(tx.hash)
    })
    .map{ tx:RpcTx => {
      val transaction_index = IngestUtil.toLong(tx.transactionIndex).toInt
      val logs:Array[RpcLog] = {
        val logs = receipts.get(tx.hash)
        if(!logs.isDefined) {
          log.warn(s"failed to find receipt logs: ${transaction_index}: ${tx.hash}")
          Array.empty
        } else {
          logs.get.logs
        }
      }
      val receipt = receipts.get(tx.hash)

      Tx(
        tx.hash,

        //IngestUtil.toLong(tx.nonce),
        IngestUtil.toBigInt(tx.nonce),
        
        transaction_index,
        formatAddr(tx.from,config.formatAddr),
        formatAddr(tx.to,config.formatAddr),
        IngestUtil.toBigInt(tx.value),
        IngestUtil.toLong(tx.gas),
        IngestUtil.toBigInt(tx.gasPrice),
        tx.input,
        tx.maxFeePerGas.map(IngestUtil.toBigInt(_)),
        tx.maxPriorityFeePerGas.map(IngestUtil.toBigInt(_)),
        tx.`type`.map(r => IngestUtil.toLong(r).toInt),
                        
        receipt.map(r => IngestUtil.toLong(r.cumulativeGasUsed)).getOrElse(0L), //0L,//tx.receipt_cumulative_gas_used, 
        receipt.map(r => IngestUtil.toLong(r.gasUsed)).getOrElse(0L), //0L,//tx.receipt_gas_used, 
        receipt.map(r => formatAddr(r.contractAddress,config.formatAddr)).flatten, //tx.receipt_contract_address, 
        Some(b.receiptsRoot), //tx.receipt_root, 
        receipt.flatMap(r => r.status.map(IngestUtil.toLong(_).toInt)), //tx.receipt_status, 
        receipt.map(_.effectiveGasPrice.map(r => IngestUtil.toBigInt(r))).flatten, //tx.receipt_effective_gas_price

        block = block,

        logs = logs.map( r => {
          LogTx(
            IngestUtil.toLong(r.logIndex).toInt,
            formatAddr(r.address,config.formatAddr),
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
