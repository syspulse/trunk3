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
import io.syspulse.haas.ingest.icp.Transaction
import io.syspulse.haas.ingest.icp.Operation
import io.syspulse.haas.ingest.icp.IcpJson._

import io.syspulse.haas.ingest.icp.flow.rosetta._
import io.syspulse.haas.ingest.icp.flow.rosetta.IcpRpcJson._


abstract class PipelineIcpTransaction[E <: skel.Ingestable](config:Config)
                                                     (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineIcp[IcpRpcTransaction,IcpRpcTransaction,E](config) {
    
  def apiSuffix():String = ""

  def parse(data:String):Seq[IcpRpcTransaction] = {
    val bb = parseBlock(data)    
   
    if(bb.size!=0) {
      latestTs.set(bb.last.block.timestamp * 1000L)
      
      bb.flatMap(b => {
        b.block.transactions
      })

    } else
      Seq()
  }

  def convert(tx:IcpRpcTransaction):IcpRpcTransaction = {
    tx
  }

  // def transform(block: Block): Seq[Block] = {
  //   Seq(block)
  // }
}

class PipelineTansaction(config:Config) extends PipelineIcpTransaction[Transaction](config) {    

  def transform(tx: IcpRpcTransaction): Seq[Transaction] = {

    val from = tx.operations.find(p => 
      (p.`type`.toUpperCase == "TRANSACTION" && p.status.toUpperCase() == "COMPLETED" && p.amount.value.trim.startsWith("-")) ||
      (p.`type`.toUpperCase == "TRANSFER" && p.status.toUpperCase() == "REVERTED")
    )

    val to = tx.operations.find(p => 
      (p.`type`.toUpperCase == "TRANSACTION" && p.status.toUpperCase() == "COMPLETED" && !p.amount.value.trim.startsWith("-"))      
    )

    val fee = tx.operations.find(p => 
      (p.`type`.toUpperCase == "FEE" && p.status.toUpperCase() == "COMPLETED")
    ).map(_.amount.value.stripPrefix("-"))

    val value = from.map(f => f.amount.value.stripPrefix("-"))

    val t = Transaction(
      ts = tx.metadata.timestamp / 1000000L,
      hash = tx.transaction_identifier.hash,
      blk = tx.metadata.block_height,

      from = from.map(_.account.address),
      to = to.map(_.account.address),
      fee = fee.map(BigInt(_)).getOrElse(0),
      v = value.map(BigInt(_)).getOrElse(0),

      alw = None,
      alwe = None,

      spend = None,
      
      typ = from.map(_.`type`).getOrElse(""),
      memo = tx.metadata.memo.toString,
      icrc1 = None,
      
      exp = None,

    )

    // commit cursor
    cursor.commit(t.blk)

    Seq(t)
  }    
}
