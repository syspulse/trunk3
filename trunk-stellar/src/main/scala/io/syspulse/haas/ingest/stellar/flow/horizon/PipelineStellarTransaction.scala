package io.syspulse.haas.ingest.stellar.flow.horizon

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

import io.syspulse.haas.ingest.stellar.Block
import io.syspulse.haas.ingest.stellar.Transaction
import io.syspulse.haas.ingest.stellar.Operation
import io.syspulse.haas.ingest.stellar.StellarJson._

import io.syspulse.haas.ingest.stellar.flow.horizon._
import io.syspulse.haas.ingest.stellar.flow.horizon.StellarRpcJson._

abstract class PipelineStellarTransaction[E <: skel.Ingestable](config:Config)
                                                     (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineStellar[StellarRpcBlock,StellarRpcBlock,E](config) {
    
  def apiSuffix():String = ""

  def parse(data:String):Seq[StellarRpcBlock] = {
    val bb = parseBlock(data)    
    if(bb.size!=0) {
      val b = bb.last
      val ts = parseTs(b.closed_at)
      latestTs.set(ts)
    }
    bb
  }

  def convert(block:StellarRpcBlock):StellarRpcBlock = {
    block
  }

  // def transform(block: Block): Seq[Block] = {
  //   Seq(block)
  // }
}

class PipelineTransaction(config:Config) extends PipelineStellarTransaction[Transaction](config) {    

  def transform(b: StellarRpcBlock): Seq[Transaction] = {
         
    val txx = decodeTransactions(b).map( t => {
      Transaction(
        ts = parseTs(t.created_at),
        hash = t.id,
        blk = t.ledger,

        from = t.source_account,
        to = None,
        fee = BigInt(t.fee_charged),
        v = BigInt(0),
      )
    })
      
    // commit cursor
    cursor.commit(b.sequence)
    txx
  }    
}
