package io.syspulse.haas.ingest.starknet.flow.rpc

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

import io.syspulse.haas.ingest.starknet.Block
import io.syspulse.haas.ingest.starknet.StarknetJson._

import io.syspulse.haas.ingest.starknet.flow.rpc._
import io.syspulse.haas.ingest.starknet.flow.rpc.StarknetRpcJson._


abstract class PipelineStarknetBlock[E <: skel.Ingestable](config:Config)
                                                     (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineStarknet[RpcBlock,RpcBlock,E](config) {
    
  def apiSuffix():String = s"/block"

  def parse(data:String):Seq[RpcBlock] = {
    val bb = parseBlock(data)    
    if(bb.size!=0) {
      val b = bb.last.result.get
      latestTs.set(b.timestamp * 1000L)      
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

class PipelineBlock(config:Config) extends PipelineStarknetBlock[Block](config) {    

  def transform(block: RpcBlock): Seq[Block] = {
    val b = block.result.get

    val blk = Block(
      b.block_number,
      b.block_hash,      
      b.parent_hash,            
      b.sequencer_address,
      b.status,
      b.new_root,
      b.timestamp * 1000L,
      tx = None,
      l1gas = b.l1_gas_price.map(g => toBigInt(g.price_in_wei))
    )

    // commit cursor
    cursor.commit(blk.i)

    Seq(blk)
  }    
}
