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
import io.syspulse.haas.ingest.starknet.Tx
import io.syspulse.haas.ingest.starknet.EventTx
import io.syspulse.haas.ingest.starknet.StarknetJson._

import io.syspulse.haas.ingest.starknet.flow.rpc._
import io.syspulse.haas.ingest.starknet.flow.rpc.StarknetRpcJson._


abstract class PipelineStarknetTx[E <: skel.Ingestable](config:Config)
                                                     (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineStarknet[RpcBlock,RpcBlock,E](config) {
    
  def apiSuffix():String = s"/tx"

  def parse(data:String):Seq[RpcBlock] = {
    val bb = parseBlock(data)    
    if(bb.size!=0) {
      val b = bb.last.result.get
      latestTs.set(b.timestamp * 1000L)      
    }
    bb.flatMap(_.result)
  }

  def convert(block:RpcBlock):RpcBlock = {
    block
  }

  // def transform(block: Block): Seq[Block] = {
  //   Seq(block)
  // }
}

class PipelineTx(config:Config) extends PipelineStarknetTx[Tx](config) {    

  def transform(blk: RpcBlock): Seq[Tx] = {
    val b = blk

    val ts = b.timestamp
    val block_number = b.block_number
         
    val receipts:Map[String,RpcReceipt] = decodeReceipts(blk)
    
    val numEvents = receipts.values.foldLeft(0)((c,r) => c + r.events.size)
    val numTransfers = b.transactions.foldLeft(0)((c,t) => c + {if(! t.calldata.isDefined) 0 else 1})
    val numCalls = b.transactions.size - numTransfers
    log.info(s"Block(${block_number},${b.transactions.size},${receipts.size},${numEvents},${numTransfers},${numCalls})")

    if(receipts.size != b.transactions.size) {
      log.error(s"transactions: ${b.transactions.size}, receipts: ${receipts.size}")
      return Seq()
    }

    val block = Block(
      b.block_number,
      b.block_hash,      
      b.parent_hash,            
      b.sequencer_address,
      b.status,
      b.new_root,
      b.timestamp * 1000L,
      tx = None,
      l1gas = b.l1_gas_price.map(g => toBigInt(g.price_in_wei))
    )

    val txx = b.transactions.view.zipWithIndex.map{ case(tx,i) => {
      val transaction_index = i
      val events:Array[RpcEvent] = {
        val events = receipts.get(tx.transaction_hash)
        if(!events.isDefined) {
          log.warn(s"failed to find receipt events: ${transaction_index}: ${tx.transaction_hash}")
          Array.empty
        } else {
          events.get.events
        }
      }
      val receipt = receipts.get(tx.transaction_hash)      

      Tx(
        hash = tx.transaction_hash,
        nonce = toLong(tx.nonce),
        from = tx.sender_address.getOrElse(""),
        fee = tx.max_fee.map(f => toBigInt(f)),
        typ = tx.`type`,
        ver = toLong(tx.version).toInt,
        sig = tx.signature.getOrElse(Array.empty).mkString(":"),
        data = tx.calldata.getOrElse(Array.empty),
        entry = tx.entry_point_selector,

        fee1 = receipt.get.actual_fee,
        st = receipt.get.execution_status,
        fin = receipt.get.finality_status,
        //msgs: Seq[MessageL1],   // messages sent

        block = block,

        events = events.map( r => {
          EventTx(
            data = r.data,
            from = r.from_address,
            keys = r.keys
          )
        })
      )
    }}.toSeq

    if(receipts.size == b.transactions.size) {
      // commit cursor only if all transactions receipts recevied !
      cursor.commit(block_number)
    }

    txx
  }
}
