package io.syspulse.haas.ingest.vechain.flow.rpc

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

import io.syspulse.haas.ingest.vechain.Block
import io.syspulse.haas.ingest.vechain.VechainJson._

import io.syspulse.haas.ingest.vechain.flow.rpc._
import io.syspulse.haas.ingest.vechain.flow.rpc.VechainRpcJson._


abstract class PipelineVechainBlock[E <: skel.Ingestable](config:Config)
                                                     (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineVechain[RpcBlock,RpcBlock,E](config) {
    
  def apiSuffix():String = s"/block"

  def parse(data:String):Seq[RpcBlock] = {
    val bb = parseBlock(data)    
    if(bb.size!=0) {
      val b = bb.last
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

class PipelineBlock(config:Config) extends PipelineVechainBlock[Block](config) {    

  def transform(block: RpcBlock): Seq[Block] = {
    val b = block

    val blk = Block(
      ts = b.timestamp * 1000L,
      i = b.number,
      hash = b.id,
      phash = b.parentID,
      sz = b.size,

      gas = b.gasLimit,
      ben = b.beneficiary,
      used = b.gasUsed,
      scor = b.totalScore,
      troot = b.txsRoot,
      feat = b.txsFeatures,
      sroot = b.stateRoot,
      rroot = b.receiptsRoot,
      com = b.com,
      signr = b.signer,

      trnk = b.isTrunk,
      fin = b.isFinalized,
      
      tx = None
    )

    // commit cursor
    cursor.commit(blk.i)

    Seq(blk)
  }    
}
