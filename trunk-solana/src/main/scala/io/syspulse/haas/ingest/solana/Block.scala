package io.syspulse.haas.ingest.solana

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class Block(
  i:Long,       // block number
  ts:Long,      // timestamp
  hash:String,  // block hash 
  phash:String, // parent hash
  i0:Long,      // parent block 

  tx:Option[Array[Transaction]], // transactions  

) extends Ingestable {
  override def getKey:Option[Any] = Some(i)

  override def toString = Util.toStringWithArray(this)
}
