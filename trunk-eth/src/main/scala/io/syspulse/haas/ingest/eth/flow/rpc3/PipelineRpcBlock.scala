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
import io.syspulse.haas.ingest.eth.BlockJson
import io.syspulse.haas.ingest.eth.BlockJson._

import io.syspulse.haas.ingest.Config

import io.syspulse.haas.ingest.eth.flow.rpc3._
import io.syspulse.skel.blockchain.eth.EthUtil

abstract class PipelineRpcBlock[E <: skel.Ingestable](config:Config)
                                                     (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineRPC[RpcBlock,RpcBlock,E](config) {  

  def apiSuffix():String = s"/block"

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

  // def transform(block: Block): Seq[Block] = {
  //   Seq(block)
  // }
}

class PipelineBlock(config:Config) extends PipelineRpcBlock[Block](config) {

  def transform(block: RpcBlock): Seq[Block] = {
    val b = block.result.get
    
    if(config.filter.size != 0 && config.filter.contains(b.hash)) {
      return Seq()
    }

    val blk = Block(
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
    
    // commit cursor
    cursor.commit(EthUtil.toLong(b.number))

    Seq(blk)
  }    
}
