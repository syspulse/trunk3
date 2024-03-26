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
import io.syspulse.haas.ingest.solana.Transaction
import io.syspulse.haas.ingest.solana.SolanaJson._

import io.syspulse.haas.ingest.solana.flow.rpc._
import io.syspulse.haas.ingest.solana.flow.rpc.SolanaRpcJson._


abstract class PipelineSolanaTransaction[E <: skel.Ingestable](config:Config)
                                                     (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineSolana[RpcBlock,RpcBlock,E](config) {
    
  def apiSuffix():String = s"/transaction"

  def parse(data:String):Seq[RpcBlock] = {
    val bb = parseBlock(data)
    if(bb.size!=0) {
      val b = bb.last.result.get
      latestTs.set(b.blockTime * 1000L)      
    }
    bb.flatMap(_.result)
  }

  def convert(tx:RpcBlock):RpcBlock = {
    tx
  }

}

class PipelineTransaction(config:Config) extends PipelineSolanaTransaction[Transaction](config) {    

  def transform(block: RpcBlock): Seq[Transaction] = {
    var i = 0;
    val txx = block.transactions.flatMap(tx => {
      tx.transaction.signatures.map( sig => {
        val t = Transaction(
          sig = sig, 
          acc = tx.transaction.message.accountKeys,
          ver = tx.version,
      
          b = Some(block.blockHeight),           // block number
          ts = Some(block.blockTime),          // timestamp
      
          i = Some(i), 
        )        
        i = i + 1
        t
      })      
    })    

    // commit cursor
    cursor.commit(block.blockTime * 1000L)

    txx
  }    
}
