package io.syspulse.haas.ingest.icp

import io.syspulse.skel.Ingestable

case class Block(
  i:Long,       // block number
  hash:String,  // block hash 
  phash:String, // parent hash
  ts:Long,       // timestamp
  tx:Option[Seq[Transaction]], // transactions

) extends Ingestable {
  override def getKey:Option[Any] = Some(i)
}
