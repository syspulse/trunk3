package io.syspulse.haas.ingest.eth.flow.rpc

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

import io.syspulse.haas.core.Block
import io.syspulse.haas.serde.BlockJson
import io.syspulse.haas.serde.BlockJson._
import io.syspulse.haas.ingest.Config
import io.syspulse.haas.ingest.eth.rpc._
import io.syspulse.haas.ingest.eth.rpc.EthRpcJson._

import io.syspulse.haas.ingest.eth.flow.rpc.LastBlock

abstract class PipelineRpcBlock[E <: skel.Ingestable](config:Config)
                                                     (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineRPC[RpcBlock,Block,E](config) {
  
  def apiSuffix():String = s"/block"

  def parse(data:String):Seq[RpcBlock] = {
    val bb = parseBlock(data)    
    if(bb.size!=0) {
      val b = bb.last.result.get
      latestTs.set(toLong(b.timestamp) * 1000L)
      
      val blockNum = toLong(b.number)
      
      // check behind
      val behind = lastBlock.isBehind(blockNum)
      if(behind >= 1) {
        log.warn(s"Behind: ${behind}: next=${lastBlock.next()}, new=${blockNum}")
        throw new RetryException(s"blocks behind: '${behind}'")
      }

      // check reorg
      val reorgs = lastBlock.isReorg(blockNum,b.hash)
      if(reorgs.size > 0) {
        // apply reorg
        log.warn(s"Blockchain Reorg: [${blockNum}/${b.hash},${b.transactions.size},${Util.tsToString(toLong(b.timestamp) * 1000L)}] ======> : depth=${reorgs.size}: ${reorgs}")
        lastBlock.reorg(reorgs)
      }

      val already = lastBlock.commit(blockNum,b.hash,toLong(b.timestamp),b.transactions.size)
      if(already) {
        throw new RetryException(s"block already committed: '${toLong(b.number)}'")
      }
    }

    bb
  }

  def convert(block:RpcBlock):Block = {
    val b = block.result.get
    val blk = Block(
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
    )
        
    blk
  }

  // def transform(block: Block): Seq[Block] = {
  //   Seq(block)
  // }
}

class PipelineBlock(config:Config) 
  extends PipelineRpcBlock[Block](config) {

  def transform(block: Block): Seq[Block] = {
    Seq(block)
  }    
}
