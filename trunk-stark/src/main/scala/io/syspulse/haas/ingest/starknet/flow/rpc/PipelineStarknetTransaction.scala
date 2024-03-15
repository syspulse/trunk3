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
import io.syspulse.haas.ingest.starknet.Transaction
import io.syspulse.haas.ingest.starknet.StarknetJson._

import io.syspulse.haas.ingest.starknet.flow.rpc._
import io.syspulse.haas.ingest.starknet.flow.rpc.StarknetRpcJson._


abstract class PipelineStarknetTransaction[E <: skel.Ingestable](config:Config)
                                                     (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineStarknet[RpcTx,RpcTx,E](config) {
    
  def apiSuffix():String = s"/transaction"

  def parse(data:String):Seq[RpcTx] = {
    val bb = parseBlock(data)    
   
    if(bb.size!=0) {
      latestTs.set(bb.last.result.get.timestamp * 1000L)
      
      bb.flatMap(b => {
        b.result.get.transactions.map(
          t => t.copy(block_number = Some(b.result.get.block_number), timestamp = Some(b.result.get.timestamp) 
        ))
      })

    } else
      Seq()
  }

  def convert(tx:RpcTx):RpcTx = {
    tx
  }

  // def transform(block: Block): Seq[Block] = {
  //   Seq(block)
  // }
}

class PipelineTransaction(config:Config) extends PipelineStarknetTransaction[Transaction](config) {    

  def transform(tx: RpcTx): Seq[Transaction] = {
    val t = Transaction(            
      hash = tx.transaction_hash,
      nonce = toLong(tx.nonce),
      from = tx.sender_address.getOrElse(""),
      fee = tx.max_fee.map(f => toBigInt(f)),
      typ = tx.`type`,
      ver = toLong(tx.version).toInt,
      sig = tx.signature.getOrElse(Array.empty).mkString(":"),
      data = tx.calldata.getOrElse(Array.empty),
      entry = tx.entry_point_selector,
      
      b = tx.block_number.getOrElse(0L),
      ts = tx.timestamp.getOrElse(0L),
    )

    // commit cursor
    cursor.commit(t.b)

    Seq(t)
  }    
}
