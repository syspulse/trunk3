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

abstract class PipelineRpcTx[E <: skel.Ingestable](config:Config)
                                                  (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineRPC[RpcBlock,RpcBlock,E](config) {
  
  def apiSuffix():String = s"/tx"

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

class PipelineTx(config:Config) extends PipelineRpcTx[Tx](config) {
  import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson._
  
  def transform(block: RpcBlock): Seq[Tx] = {
    val b = block.result.get

    val ts = toLong(b.timestamp)
    val block_number = toLong(b.number)

    val json = 
    "[" + b.transactions.map( t => 
      s"""{"jsonrpc":"2.0","method":"eth_getTransactionReceipt","params":["${t.hash}"],"id":"${t.hash}"}"""
     ).mkString(",") +
    "]"
    .trim.replaceAll("\\s+","")

    log.info(s"transaction: ${b.transactions.size}")
      
    val receipts:Map[String,RpcReceipt] = if(b.transactions.size > 0) {
      val receiptsRsp = requests.post(config.feed, data = json,headers = Map("content-type" -> "application/json"))
      val receipts:Map[String,RpcReceipt] = receiptsRsp.statusCode match {
        case 200 =>
          // need to import it here for List[]

          val batchRsp = receiptsRsp.data.toString

          try {
            val batchReceipts = batchRsp.parseJson.convertTo[List[RpcReceiptResultBatch]]

            val rr:Seq[RpcReceipt] = batchReceipts.flatMap { r => 
              
              if(r.result.isDefined) {
                Some(r.result.get)
              } else {
                log.warn(s"could not get receipt: (tx=${r.id}): ${r}")
                None
              }
            }

            // commit cursor
            cursor.commit(toLong(b.number))

            rr.map( r => r.transactionHash -> r).toMap

          } catch {
            case e:Exception =>
              log.error(s"could not parse receipts batch: ${receiptsRsp}",e)
              Map()
          }
        case _ => 
          log.warn(s"could not get receipts batch: ${receiptsRsp}")
          Map()
      }
      receipts
    } else
      Map()

    b.transactions.map{ tx:RpcTx => {
      val transaction_index = toLong(tx.transactionIndex).toInt
      val logs = receipts.get(tx.hash).get.logs
      Tx(
        // ts * 1000L,
        transaction_index,
        tx.hash,
        // block_number,

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

        receipts.get(tx.hash).map(r => toLong(r.cumulativeGasUsed)).getOrElse(0L), //0L,//tx.receipt_cumulative_gas_used, 
        receipts.get(tx.hash).map(r => toLong(r.gasUsed)).getOrElse(0L), //0L,//tx.receipt_gas_used, 
        receipts.get(tx.hash).map(_.contractAddress).flatten, //tx.receipt_contract_address, 
        Some(b.receiptsRoot), //tx.receipt_root, 
        receipts.get(tx.hash).map(r => toLong(r.status).toInt), //tx.receipt_status, 
        receipts.get(tx.hash).map(_.effectiveGasPrice.map(r => toBigInt(r))).flatten, //tx.receipt_effective_gas_price

        block = Block(
          toLong(b.number),
          b.hash,
          b.parentHash,
          b.nonce,
          b.sha3Uncles,        
          b.logsBloom,
          b.transactionsRoot,
          b.stateRoot,        
          b.receiptsRoot,
          b.miner,
          
          toBigInt(b.difficulty),
          toBigInt(b.totalDifficulty),
          toLong(b.size),

          b.extraData, 
              
          toLong(b.gasLimit), 
          toLong(b.gasUsed), 
          toLong(b.timestamp) * 1000L, 
          b.transactions.size,
          b.baseFeePerGas.map(d => toLong(d))
        ),

        logs = logs.map( r => {
          EventTx(
            toLong(r.logIndex).toInt,
            r.address,
            r.data,
            r.topics
          )
        })
      )
    }}.toSeq
  }
}
