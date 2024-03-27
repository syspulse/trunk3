package io.syspulse.haas.ingest.solana

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class Transaction(  
  ts:Option[Long],          // timestamp
  s:Option[Long],           // block slot
  b:Option[Long],           // block number

  sig:String,      // transaction signature  
  acc:Array[String],        // account key

  units:Long,               // consumed units
  fee:Long,                 // fee
  logs:Array[String],       // log messsages
  st:String,                // status

  ver:String,  
  i:Option[Long] = None,  // transaction index in Block
  
) extends Ingestable {
  override def getKey:Option[Any] = Some(sig)

  override def toString = Util.toStringWithArray(this)
}
