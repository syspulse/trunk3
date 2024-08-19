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

import io.syspulse.haas.ingest.eth.TokenTransfer
import io.syspulse.haas.ingest.eth.TokenTransferJson._

import io.syspulse.haas.ingest.Config
import io.syspulse.haas.ingest.eth.flow.rpc3._
import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson
import io.syspulse.haas.ingest.IngestUtil

abstract class PipelineRpcTokenTransfer[E <: skel.Ingestable](config:Config)
                (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineRPC[RpcBlock,RpcBlock,E](config) {
  
  def apiSuffix():String = s"/token-transfer"

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

class PipelineTokenTransfer(config:Config) extends PipelineRpcTokenTransfer[TokenTransfer](config) {
  import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson._
  
  val TOPIC_DATA_ADDR_PREFIX = "0x000000000000000000000000"

  def transform(block: RpcBlock): Seq[TokenTransfer] = {
    val b = block.result.get

    val ts = IngestUtil.toLong(b.timestamp)
    val block_number = IngestUtil.toLong(b.number)
    
    log.info(s"transaction: ${b.transactions.size}")
      
    val receipts:Map[String,RpcReceipt] = decodeReceipts(block)
    
    val tt = b.transactions.flatMap( tx => {
      val transaction_index = IngestUtil.toLong(tx.transactionIndex).toInt
      val receipt = receipts(tx.hash)
      val logs = receipt.logs
      
      logs
      .filter( log => {
        log.topics.size >= 3 && { log.topics(0).toLowerCase().take(8 * 2 + 2) match {
          // 0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef
          case "0xddf252ad1be2c89b" => true
          case _ => false
        }}
      })
      .map( log => {        
        val from = s"0x${log.topics(1).drop(TOPIC_DATA_ADDR_PREFIX.size)}"
        val to = s"0x${log.topics(2).drop(TOPIC_DATA_ADDR_PREFIX.size)}"
        val value = if(log.topics.size == 4)
          IngestUtil.toBigInt(log.topics(3))
        else
          IngestUtil.toBigInt(log.data)

        TokenTransfer(
          ts = ts * 1000L,
          blk = block_number,
          con = formatAddr(log.address),

          from = formatAddr(from),
          to = formatAddr(to),
          v = value,

          hash = tx.hash,                                 // transaction hash !
          i = IngestUtil.toLong(log.logIndex).toInt,                 // log index
        )
      })
      
    }).toSeq

    if(receipts.size == b.transactions.size) {
      // commit cursor only if all transactions receipts recevied !
      cursor.commit(block_number)
    }

    tt
  }
}
