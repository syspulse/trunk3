package io.syspulse.haas.ingest.solana

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class Block(
  ts:Long,      // timestamp
  s:Long,       // slot
  i:Long,       // block number
  s0:Long,      // parent block  

  hash:String,  // block hash 
  phash:String, // parent hash  

  tx:Option[Array[Transaction]], // transactions  

) extends Ingestable {
  override def getKey:Option[Any] = Some(i)

  override def toString = Util.toStringWithArray(this)
}
