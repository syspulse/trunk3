package io.syspulse.haas.ingest.icp.flow.rosetta

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

import io.syspulse.haas.ingest.icp.Block
import io.syspulse.haas.ingest.icp.IcpJson._

import io.syspulse.haas.ingest.icp.flow.rosetta._
import io.syspulse.haas.ingest.icp.flow.rosetta.IcpRpcJson._

abstract class PipelineIcpBlock[E <: skel.Ingestable](config:Config)
                                                     (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineIcp[IcpRpcBlock,IcpRpcBlock,E](config) {
    
  def apiSuffix():String = ""

  def parse(data:String):Seq[IcpRpcBlock] = {
    val bb = parseBlock(data)    
    if(bb.size!=0) {
      val b = bb.last.block
      latestTs.set(b.timestamp * 1000L)      
    }
    bb
  }

  def convert(block:IcpRpcBlock):IcpRpcBlock = {
    block
  }

  // def transform(block: Block): Seq[Block] = {
  //   Seq(block)
  // }
}

class PipelineBlock(config:Config) extends PipelineIcpBlock[Block](config) {    

  def transform(b: IcpRpcBlock): Seq[Block] = {
    val block = Block(
      b.block.block_identifier.index,
      b.block.timestamp * 1000L,
      b.block.block_identifier.hash,
      b.block.parent_block_identifier.hash,
      
      tx = None
    )

    // commit cursor
    cursor.commit(block.i)

    Seq(block)
  }    
}
