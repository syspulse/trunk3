package io.syspulse.haas.ingest.bitcoin

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class Block(
  ts:Long,       // timestamp
  i:Long,       // block number  
  hash:String,  // block hash 
  phash:String, // parent hash  
  
  //tx:Option[Array[Transaction]], // transactions  

) extends Ingestable {
  override def getKey:Option[Any] = Some(i)

  override def toString = Util.toStringWithArray(this)
}
