package io.haas.ingest.solana

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util
import io.haas.ingest.solana.flow.rpc.RpcInstruction
import io.haas.ingest.ext.{BlockLike,TxLike}

case class Tx(  
  
  acc:Array[String],        // account key
  unts:Long,                // consumed units (legacy)
  fee:Long,                 // fee (lamports)
  err:Option[String] = None,// error (stringified json if present)
  used:Long = 0L,           // computeUnitsConsumed
  cost:Long = 0L,           // costUnits
  bal1:Option[Array[Long]] = None,          // postBalances
  bal0:Option[Array[Long]] = None,          // preBalances
  tok0:Option[Array[TokBal]] = None,        // postTokenBalances
  tok1:Option[Array[TokBal]] = None,        // preTokenBalances

  ins:Array[RpcInstruction], // instructions
  logs:Array[String],        // log messages

  sig:String,                // transaction signature 

  sta:String,                // status
  ver:String,                // version
  i:Option[Long] = None,  // transaction index in Block

  block:Block,               // block
  
) extends Ingestable with TxLike {

  override def getKey:Option[Any] = Some(sig)
  override def hash:String = sig
  override def timestamp:Long = block.ts
  override def index:Long = i.getOrElse(0L)
  override def block_number:Long = block.b
  override def transaction_count:Long = block.tx.map(_.size.toLong).getOrElse(0L)
  override def sim:Option[String] = None
  override def getBlock[B <: BlockLike]():Option[B] = Some(block.asInstanceOf[B])
  override def emptyBlock[B <: BlockLike]():Option[B] = Some(block.emptyBlock().asInstanceOf[B])

  override def toString = Util.toStringWithArray(this)
}

