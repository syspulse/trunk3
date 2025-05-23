package io.syspulse.haas.ingest.bitcoin.flow.rpc

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import scala.util.{Try,Success,Failure}
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

import io.syspulse.haas.ingest.bitcoin.flow.rpc.RpcBlock
import io.syspulse.haas.ingest.bitcoin.flow.rpc.RpcJsonProtocol._
import io.syspulse.haas.ingest.bitcoin.{Block}
import io.syspulse.haas.ingest.bitcoin.BitcoinJson._

import java.util.concurrent.TimeUnit

import io.syspulse.haas.ingest.Config

abstract class PipelineRpcBlock[E <: skel.Ingestable](config:Config)
  (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineRPC[RpcBlock,RpcBlock,E](config) {  

  def apiSuffix():String = s"/block"

  def parse(data:String):Seq[RpcBlock] = {
    rpc.parseBlock(data) match {
      case Success(b) => 
        latestTs.set(b.time * 1000L)      
        Seq(b)
      case Failure(e) => 
        log.error(s"failed to parse block: ${e.getMessage}")
        Seq.empty
    }    
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
        
    val b = block
    if(config.filter.size != 0 && config.filter.contains(b.hash)) {
      return Seq()
    }

    val blk = Block(
      ts = b.time * 1000L,
      i = b.height,
      hash = b.hash,
      phash = b.previousblockhash,      
      c = b.confirmations,
      ver = b.version,
      merkle = b.merkleroot,
      ts_m = b.mediantime,
      nonce = b.nonce,
      bits = b.bits,
      d = b.difficulty,
      cwork = b.chainwork,
      n = b.nTx,
      nhash = b.nextblockhash,
      ssz = b.strippedsize,
      sz = b.size,
      w = b.weight,
      tx = None
    )
    
    // commit cursor
    cursor.commit(b.height)

    Seq(blk)
  }    
}
