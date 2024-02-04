package io.syspulse.haas.ingest.stellar

import io.syspulse.skel.Ingestable

case class Block(
  i:Long,       // block number
  ts:Long,       // timestamp
  hash:String,  // block hash 
  phash:String, // parent hash  
  tx:Option[Seq[Transaction]], // transactions

) extends Ingestable {
  override def getKey:Option[Any] = Some(i)
}
