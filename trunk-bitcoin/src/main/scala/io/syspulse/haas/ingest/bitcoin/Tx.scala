package io.syspulse.haas.ingest.bitcoin

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class Tx(  
  ts:Long,
  txid:String,      // txid 
  hash:String,      // transaction hash
  
  from:String,
  to:String,  
  v:BigInt,
  fee:Option[BigInt],

  block:Block,  
  
  i:Option[Long] = None,  // transaction index in Block

) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)

  override def toString = Util.toStringWithArray(this)
}
