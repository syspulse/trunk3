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

import io.syspulse.haas.ingest.eth.Event
import io.syspulse.haas.ingest.eth.EventJson._

import io.syspulse.haas.ingest.Config
import io.syspulse.haas.ingest.eth.flow.rpc3._
import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson

abstract class PipelineRpcEvent[E <: skel.Ingestable](config:Config)
                (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineRPC[RpcBlock,RpcBlock,E](config) {
  
  def apiSuffix():String = s"/log"

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

class PipelineEvent(config:Config) extends PipelineRpcEvent[Event](config) {
  import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson._
  
  def transform(block: RpcBlock): Seq[Event] = {
    val b = block.result.get

    val ts = toLong(b.timestamp)
    val block_number = toLong(b.number)

    log.info(s"transaction: ${b.transactions.size}")
      
    val receipts:Map[String,RpcReceipt] = decodeReceipts(block)
    
    val ee = b.transactions.flatMap( tx => {
      val transaction_index = toLong(tx.transactionIndex).toInt
      val receipt = receipts(tx.hash)
      val logs = receipt.logs
      
      logs.map( log => {
        Event(
          ts = ts * 1000L,
          blk = block_number,
          con = formatAddr(receipt.contractAddress.getOrElse("")),
          data = log.data,
          hash = tx.hash,                                 // transaction hash !
          topics = log.topics, 
          i = toLong(log.logIndex).toInt,                 // log index
          tix = toLong(tx.transactionIndex).toInt         // transaction index          
        )
      })
      
    }).toSeq

    if(receipts.size == b.transactions.size) {
      // commit cursor only if all transactions receipts recevied !
      cursor.commit(block_number)
    }

    ee
  }
}
