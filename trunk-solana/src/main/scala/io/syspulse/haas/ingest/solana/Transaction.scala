package io.syspulse.haas.ingest.solana

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class Transaction(  
  sig:String,      // transaction signature
  acc:Array[String],
  ver:String,
  
  b:Option[Long],           // block number
  ts:Option[Long],          // timestamp
  
  i:Option[Long] = None,  // transaction index in Block

) extends Ingestable {
  override def getKey:Option[Any] = Some(sig)

  override def toString = Util.toStringWithArray(this)
}
