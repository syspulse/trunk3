package io.syspulse.haas.ingest.starknet

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class Block(
  i:Long,       // block number
  hash:String,  // block hash 
  phash:String, // parent hash
  seq:String,   // sequencer
  st:String,   // status
  nroot:String,  // new root
  ts:Long,       // timestamp
  tx:Option[Array[Transaction]], // transactions
  l1gas:Option[BigInt] = None

) extends Ingestable {
  override def getKey:Option[Any] = Some(i)

  override def toString = Util.toStringWithArray(this)
}
