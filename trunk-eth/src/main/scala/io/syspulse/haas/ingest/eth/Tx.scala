package io.syspulse.haas.ingest.eth

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

// Transaction with blockinfo
case class Tx(
  // ts:Long,            // Timestamp is in block: Block
  i:Int,
  hash:String,
  // blk:Long,          // block is in block
  from:String,
  to:Option[String],
  gas:Long,
  p:BigInt,
  inp:String,
  v:BigInt,

  //non:Long,
  non:BigInt,

  fee:Option[BigInt],
  tip:Option[BigInt], 
  typ:Option[Int],
  used2: Long,              // cumulative used
  used: Long,               // gas used
  con: Option[String],      // contract
  root: Option[String],     // receipt root
  st: Option[Int],          // status
  p0: Option[BigInt],       // price Effective

  block:Block,
  logs: Array[EventTx],

  sim:Option[String] = None, // sim

  //timestamp:Option[Long]
) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)

  // override to show Array in a nice way
  override def toString = Util.toStringWithArray(this)
}
