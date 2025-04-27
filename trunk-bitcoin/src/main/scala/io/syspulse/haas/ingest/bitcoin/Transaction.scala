package io.syspulse.haas.ingest.bitcoin

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class Transaction(  
  ts:Long,          // timestamp (millisec)
  txid:String,      // txid 
  hash:String,      // transaction hash
  
  from:String,      // from address ("COINBASE" is sued fro Block Reward transaction)
  to:String,        // to address
  v:BigInt,         // value (always in sats)
  fee:Option[BigInt], // fee (always in sats)

  ver: Int,                // Version number
  sz: Int,                 // Size in bytes
  vsz: Int,                // Virtual size
  w: Int,                  // Weight
  ts_l: Long,              // Lock time
  
  i:Option[Long] = None,  // transaction index in Block

) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)

  override def toString = Util.toStringWithArray(this)
}
