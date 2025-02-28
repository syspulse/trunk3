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
import io.syspulse.skel.blockchain.eth.EthUtil

abstract class PipelineRpcTx[E <: skel.Ingestable](config:Config)
                                                  (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineRPC[RpcBlock,RpcBlock,E](config) {
  
  def apiSuffix():String = s"/tx"

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

class PipelineTx(config:Config) extends PipelineRpcTx[Tx](config) {
  import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson._
  
  def transform(blk: RpcBlock): Seq[Tx] = {
    val b = blk.result.get

    val ts = EthUtil.toLong(b.timestamp)
    val block_number = EthUtil.toLong(b.number)

    log.info(s"Block[${block_number},${b.transactions.size}]")
      
    val receipts:Map[String,RpcReceipt] = decodeReceipts(blk)(config,uri.uri)

    val block = Block(
      EthUtil.toLong(b.number),
      b.hash,
      b.parentHash,
      b.nonce,
      b.sha3Uncles,        
      b.logsBloom,
      b.transactionsRoot,
      b.stateRoot,        
      b.receiptsRoot,
      formatAddr(b.miner,config.formatAddr),
      
      EthUtil.toBigInt(b.difficulty),
      EthUtil.toBigInt(b.totalDifficulty),
      EthUtil.toLong(b.size),

      b.extraData, 
          
      EthUtil.toLong(b.gasLimit), 
      EthUtil.toLong(b.gasUsed), 
      EthUtil.toLong(b.timestamp) * 1000L, 
      b.transactions.size,
      b.baseFeePerGas.map(d => EthUtil.toLong(d))
    )

    val txx = b.transactions
    .filter(tx => {
      config.filter.size == 0 || config.filter.contains(tx.hash)
    })
    .map{ tx:RpcTx => {
      val transaction_index = EthUtil.toLong(tx.transactionIndex).toInt
      val logs = receipts.get(tx.hash).get.logs
      val receipt = receipts.get(tx.hash)
      
      Tx(
        // ts * 1000L,
        transaction_index,
        tx.hash,
        // block_number,

        formatAddr(tx.from,config.formatAddr),
        formatAddr(tx.to,config.formatAddr),
        
        EthUtil.toLong(tx.gas),
        EthUtil.toBigInt(tx.gasPrice),
        
        tx.input,
        EthUtil.toBigInt(tx.value),
        
        //EthUtil.toLong(tx.nonce),
        EthUtil.toBigInt(tx.nonce),
        
        tx.maxFeePerGas.map(EthUtil.toBigInt(_)), //tx.max_fee_per_gas,
        tx.maxPriorityFeePerGas.map(EthUtil.toBigInt(_)), //tx.max_priority_fee_per_gas, 

        tx.`type`.map(r => EthUtil.toLong(r).toInt),

        receipt.map(r => EthUtil.toLong(r.cumulativeGasUsed)).getOrElse(0L), //0L,//tx.receipt_cumulative_gas_used, 
        receipt.map(r => EthUtil.toLong(r.gasUsed)).getOrElse(0L), //0L,//tx.receipt_gas_used, 
        receipt.map(r => formatAddr(r.contractAddress,config.formatAddr)).flatten, //tx.receipt_contract_address, 
        Some(b.receiptsRoot), //tx.receipt_root, 
        receipt.flatMap(r => r.status.map(EthUtil.toLong(_).toInt)), //tx.receipt_status, 
        receipt.map(_.effectiveGasPrice.map(r => EthUtil.toBigInt(r))).flatten, //tx.receipt_effective_gas_price

        block = block,

        logs = logs.map( r => {
          EventTx(
            EthUtil.toLong(r.logIndex).toInt,
            formatAddr(r.address,config.formatAddr),
            r.data,
            r.topics
          )
        })
      )
    }}
    .map(tx => {
      if( config.cmd == "replay" && config.filter.size > 0) {
        // replay mode, fix index and count for tx to be recognized by Detectors (must be 1 in 1 block)
        tx.copy(i = 0, block = tx.block.copy(txn = 1))

      } else
        tx
    })
    .toSeq

    if(receipts.size == b.transactions.size) {
      // commit cursor only if all transactions receipts recevied !
      cursor.commit(block_number)
    }

    txx
  }
}
