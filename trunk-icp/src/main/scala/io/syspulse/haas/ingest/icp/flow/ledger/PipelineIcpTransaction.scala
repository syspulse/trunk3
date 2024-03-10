package io.syspulse.haas.ingest.icp.flow.ledger

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
import io.syspulse.haas.ingest.icp.Transaction
import io.syspulse.haas.ingest.icp.IcpJson._

import io.syspulse.haas.ingest.icp.flow.ledger._
import io.syspulse.haas.ingest.icp.flow.ledger.IcpRpcJson._

// The concept of Blocks in Ledger API is somewhat ambiguous
// IcpRcpBlock has Transaction info inside, so every block is 1 transaction
abstract class PipelineIcpTransaction[E <: skel.Ingestable](config:Config)
                                                     (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineIcp[IcpRpcBlock,IcpRpcBlock,E](config) {
    
  def apiSuffix():String = ""

  def parse(data:String):Seq[IcpRpcBlock] = {
    val bb = parseBlock(data)    
    if(bb.size!=0) {
      val b = bb.last
      latestTs.set(b.created_at * 1000L)      
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

class PipelineTransaction(config:Config) extends PipelineIcpTransaction[Transaction](config) {    

  def transform(b: IcpRpcBlock): Seq[Transaction] = {
    val tx = Transaction(
      ts = b.created_at * 1000L,
      hash = b.transaction_hash,
      blk = b.block_height.toLong,

      from = b.from_account_identifier,
      to = b.to_account_identifier,
      fee = BigInt(b.fee),
      v = BigInt(b.amount),

      alw = b.allowance.map(BigInt(_)),
      alwe = b.expected_allowance.map(BigInt(_)),

      spend = b.spender_account_identifier,
      
      typ = b.transfer_type,
      memo = b.memo,
      icrc1 = b.icrc1_memo,
      
      exp = b.expires_at.map(_.toLong * 1000L)
    )

    // commit cursor
    cursor.commit(tx.blk)
    Seq(tx)
  }    
}
