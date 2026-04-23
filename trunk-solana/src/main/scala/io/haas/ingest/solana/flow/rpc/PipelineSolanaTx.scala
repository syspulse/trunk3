package io.haas.ingest.solana.flow.rpc

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger
import scala.collection.immutable.ArraySeq

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

import java.util.concurrent.TimeUnit

import io.haas.ingest.Config

import io.haas.ingest.solana.Block
import io.haas.ingest.solana.Tx
import io.haas.ingest.solana.{TokBal,TokUI}
import io.haas.ingest.solana.SolanaJson._

import io.haas.ingest.solana.flow.rpc._
import io.haas.ingest.solana.flow.rpc.SolanaRpcJson._

object ParqTxIgnore extends skel.serde.ParqIgnore[Tx] 
import ParqTxIgnore._

abstract class PipelineSolanaTx[E <: skel.Ingestable](config:Config)
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

class PipelineTx(config:Config) extends PipelineSolanaTx[Tx](config) {    
  
  def transform(block: RpcBlock): Seq[Tx] = {
    var i = 0L
        
    val txx = block.transactions.map(tx => {
      
      val innerIns: Array[RpcInstruction] =
        tx.meta.innerInstructions
          .getOrElse(Array.empty)
          .sortBy(_.index)
          .flatMap(_.instructions)

      val t = Tx(

        acc = accountPubKeys(tx.transaction.message.accountKeys),
        fee = tx.meta.fee,
        err = tx.meta.err match {
          case None => None
          case Some(JsNull) => None
          case Some(v) => Some(v.compactPrint)
        },
        used = tx.meta.computeUnitsConsumed,
        cost = tx.meta.costUnits,
        bal1 = Some(tx.meta.postBalances),
        bal0 = Some(tx.meta.preBalances),
        tok0 = Some(tx.meta.postTokenBalances.map(toTokBal)),
        tok1 = Some(tx.meta.preTokenBalances.map(toTokBal)),
        ins = tx.transaction.message.instructions ++ innerIns,
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

        block = Block(
          ts = block.blockTime * 1000L,
          b = block.parentSlot + 1,
          h = block.blockHeight,
          hash = block.blockhash,
          phash = block.previousBlockhash,
          tx = None
        ),
      )

      i = i + 1
      t
    })


    // commit cursor
    cursor.commit(block.parentSlot + 1)

    //log.debug(s"Block[${block.parentSlot+1},${block.transactions.size},${txx.size}]")

    ArraySeq.unsafeWrapArray(txx)
  }    
}
