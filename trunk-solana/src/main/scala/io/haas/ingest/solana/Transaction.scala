package io.haas.ingest.solana

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class Transaction(  
  ts:Option[Long],          // timestamp  
  b:Option[Long],           // block number (parent slot - 1)   
  h:Option[Long],           // block height  
  
  acc:Array[String],        // account key
  unts:Long,               // consumed units
  fee:Long,                 // fee    
  logs:Array[String],       // log messsages

  sig:String,      // transaction signature 

  sts:String,                // status
  ver:String,
  i:Option[Long] = None,  // transaction index in Block
  
) extends Ingestable {
  override def getKey:Option[Any] = Some(sig)

  override def toString = Util.toStringWithArray(this)
}
