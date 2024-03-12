package io.syspulse.haas.ingest.starknet

import io.syspulse.skel.Ingestable

case class Block(
  i:Long,       // block number
  hash:String,  // block hash 
  phash:String, // parent hash
  seq:String,   // sequencer
  sts:String,   // status
  nroot:String,  // new root
  ts:Long,       // timestamp
  tx:Option[Array[Transaction]], // transactions
  l1gas:Option[BigInt] = None

) extends Ingestable {
  override def getKey:Option[Any] = Some(i)
}
