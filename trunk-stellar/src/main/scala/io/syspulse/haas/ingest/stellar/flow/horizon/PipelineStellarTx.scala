package io.syspulse.haas.ingest.stellar.flow.horizon

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

import akka.util.ByteString

import org.stellar.sdk.xdr._
import org.stellar.sdk._

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
import io.syspulse.haas.ingest.stellar.Tx
import io.syspulse.haas.ingest.stellar.StellarJson._

import io.syspulse.haas.ingest.stellar.flow.horizon._
import io.syspulse.haas.ingest.stellar.flow.horizon.StellarRpcJson._

abstract class PipelineStellarTx[E <: skel.Ingestable](config:Config)
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

class PipelineTx(config:Config) extends PipelineStellarTransaction[Tx](config) {    

  def transform(b: StellarRpcBlock): Seq[Tx] = {
         
    var opsNum = 0
    val txx = decodeTransactions(b).zipWithIndex.map{ case(t,i) => {
      
      val transactionResult:TransactionResult = TransactionResult.fromXdrBase64(t.result_xdr)
      val st = transactionResult.getResult().getDiscriminant
      val fee = transactionResult.getFeeCharged().getInt64()

      val results = transactionResult.getResult().getResults()
      val typ = results.toString()
      // val rr = transactionResult.getResult().getResults()(0).getTr().getPaymentResult().getDiscriminant()
      // val typ = transactionResult.getResult().getResults()(0).getTr().getDiscriminant()

      //info(s">>> ${r},${f}:${t}=${rr}")
      // t should === (OperationType.PAYMENT)
      // r should === (TransactionResultCode.txSUCCESS)
      // f should === (100)
      // rr should === (PaymentResultCode.PAYMENT_SUCCESS)

      opsNum = opsNum + t.operation_count
      Tx(
        ts = parseTs(t.created_at),
        b = b.sequence,//t.ledger,
        hash = t.id,        
        st = if(t.successful) 1 else 0,              // status 0 - failed (like in ethereum)

        from = t.source_account,
        to = None,        
        v = BigInt(0),
        fee = BigInt(t.fee_charged),

        typ = typ.toString
      )

    }}

    //if(txx.size == 0) 
    {
      log.info(s"Block[${b.sequence},tx=${txx.size},ops=${opsNum}]")
    }
      
    // commit cursor
    cursor.commit(b.sequence)
    txx.toSeq
  }    
}
