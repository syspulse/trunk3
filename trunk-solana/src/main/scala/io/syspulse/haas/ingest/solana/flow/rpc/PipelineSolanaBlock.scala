package io.syspulse.haas.ingest.solana.flow.rpc

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

import akka.util.ByteString

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

import io.syspulse.haas.ingest.solana.Block
import io.syspulse.haas.ingest.solana.SolanaJson._

import io.syspulse.haas.ingest.solana.flow.rpc._
import io.syspulse.haas.ingest.solana.flow.rpc.SolanaRpcJson._


abstract class PipelineSolanaBlock[E <: skel.Ingestable](config:Config)
                                                     (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineSolana[RpcBlock,RpcBlock,E](config) {
    
  def apiSuffix():String = s"/block"

  def parse(data:String):Seq[RpcBlock] = {
    val bb = parseBlock(data)
    if(bb.size!=0) {
      val b = bb.last.result.get
      latestTs.set(b.blockTime * 1000L)      
    }
    bb.flatMap(_.result)
  }

  def convert(block:RpcBlock):RpcBlock = {
    block
  }

  // def transform(block: Block): Seq[Block] = {
  //   Seq(block)
  // }
}

class PipelineBlock(config:Config) extends PipelineSolanaBlock[Block](config) { 

  def transform(block: RpcBlock): Seq[Block] = {
    val b = block

    val blk = Block(
      ts = b.blockTime * 1000L,
      s = b.parentSlot + 1,
      i = b.blockHeight,
      s0 = b.parentSlot,
            
      hash = b.blockhash,            
      phash = b.previousBlockhash,
      
      tx = None
    )

    // ATTENTION: commit cursor to parent slot + 1
    cursor.commit(blk.s)

    Seq(blk)
  }    
}
