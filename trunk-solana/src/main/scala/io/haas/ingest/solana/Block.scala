package io.haas.ingest.solana

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class Block(
  ts:Long,      // timestamp
  b:Long,       // block number (parent slot - 1)    
  h:Long,       // block height

  hash:String,  // block hash 
  phash:String, // parent hash  

  tx:Option[Array[Transaction]], // transactions  

) extends Ingestable {
  override def getKey:Option[Any] = Some(b)

  override def toString = Util.toStringWithArray(this)
}
