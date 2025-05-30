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
import io.syspulse.haas.ingest.bitcoin.{Tx}
import io.syspulse.haas.ingest.bitcoin.BitcoinJson._

import java.util.concurrent.TimeUnit

import io.syspulse.haas.ingest.Config

abstract class PipelineRpcTx[E <: skel.Ingestable](config:Config)
  (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineRPC[RpcBlock,RpcBlock,E](config) {  

  def apiSuffix():String = s"/tx"

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


class PipelineTx(config:Config) extends PipelineRpcTx[Tx](config) {

  def transform(block: RpcBlock): Seq[Tx] = {
    
    if(config.filter.size != 0 && config.filter.contains(block.hash)) {
      return Seq()
    }

    // Use the existing decodeBlock method which already has all the logic
    // for transaction processing including prevout handling
    val txs = try {

      val logMsg = s"Block[${block.height},${block.nTx}]"
      if(block.nTx == 0)
        log.warn(logMsg + ": Empty")
      else 
        log.info(logMsg)

      rpc.decodeBlock(block.toJson.toString)
    } catch {
      case e: Throwable => 
        log.error(s"${block.height}: failed to decode block: ${e.getMessage}")
        Seq.empty
    }

    // commit cursor even if failed to decode block
    cursor.commit(block.height)

    txs
  }
}
