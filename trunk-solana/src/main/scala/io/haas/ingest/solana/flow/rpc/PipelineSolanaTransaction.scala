package io.haas.ingest.solana.flow.rpc

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
import io.syspulse.skel.serde.ParqIgnore
import io.syspulse.skel.serde.Parq._
import com.github.mjakubowski84.parquet4s.{ParquetRecordEncoder,ParquetSchemaResolver}

object ParqRpcInstructionTx extends ParqIgnore[RpcInstruction]
import ParqRpcInstructionTx._

import java.util.concurrent.TimeUnit

import io.haas.ingest.Config

import io.haas.ingest.solana.Block
import io.haas.ingest.solana.Transaction
import io.haas.ingest.solana.SolanaJson._

import io.haas.ingest.solana.flow.rpc._
import io.haas.ingest.solana.flow.rpc.SolanaRpcJson._


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

    bb
      .flatMap(_.result)
      .map(b => {
        log.info(s"Block[${b.parentSlot+1},${b.transactions.size},${data.size}]")
        b
      })      
  }

  def convert(tx:RpcBlock):RpcBlock = {
    tx
  }

}

class PipelineTransaction(config:Config) extends PipelineSolanaTransaction[Transaction](config) {    

  private def accountPubKeys(keys: Array[JsValue]): Array[String] = {
    keys.map {
      case JsString(s) => s
      case JsObject(fields) =>
        fields.get("pubkey") match {
          case Some(JsString(s)) => s
          case _ => JsObject(fields).compactPrint
        }
      case other => other.compactPrint
    }
  }

  def transform(block: RpcBlock): Seq[Transaction] = {
    var i = 0L
        
    val txx = block.transactions.map(tx => {

      val t = Transaction(
        ts = Some(block.blockTime * 1000L),
        b = Some(block.parentSlot + 1),
        h = Some(block.blockHeight),

        acc = accountPubKeys(tx.transaction.message.accountKeys),
        unts = tx.meta.computeUnitsConsumed,
        fee = tx.meta.fee,
        ins = tx.transaction.message.instructions,
        logs = tx.meta.logMessages.getOrElse(Array.empty[String]),

        // One Solana transaction object with its canonical (first) signature.
        sig = tx.transaction.signatures.headOption.getOrElse(""),

        sta = parseStatus(tx.meta.status),

        ver = tx.version match {
          case Some(JsString(s)) => s
          case Some(JsNumber(n)) => n.toString
          case Some(JsNull) => "legacy"
          case _ => "legacy"
        },

        i = Some(i),
      )

      i = i + 1
      t
    })


    // commit cursor
    cursor.commit(block.parentSlot + 1)

    log.debug(s"Block[${block.parentSlot+1},${block.transactions.size},${txx.size}]")

    txx
  }    
}
