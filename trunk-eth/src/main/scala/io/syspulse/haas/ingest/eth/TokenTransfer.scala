package io.syspulse.haas.ingest.eth

import io.syspulse.skel.Ingestable

case class TokenTransfer(
  ts:Long,
  blk:Long,
  con:String,
  from:String,
  to:String,
  v:BigInt,
  hash:String,
  i:Int
  
) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
}
