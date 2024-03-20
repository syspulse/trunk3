package io.syspulse.haas.ingest.starknet

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class EventTx(
  data: Array[String],
  from: String,
  keys: Array[String]
) {
  override def toString = Util.toStringWithArray(this)
}

case class Tx(  
  hash:String,      // transaction hash
  nonce:Long,
  from:String,
  fee:Option[BigInt],
  typ:String,
  ver:Int,
  sig:String,               // signature (can be empty)
  
  data:Array[String],       // calldata  
  entry:Option[String],     // entry_point_selector

  fee1: String,             // actual fee
  st: String,               // execution status
  fin: String,              // finality status
  //msgs: Seq[MessageL1],   // messages sent

  block:Block,
  events:Array[EventTx],
  
  i:Option[Long] = None,  // transaction index in Block

) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)

  override def toString = Util.toStringWithArray(this)
}
