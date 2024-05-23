package io.syspulse.haas.ingest.vechain

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class EventTx(
  address:String,  // contract address
  data:String,      // data 
  topics:Array[String] = Array(), // topics
) {
  override def toString = Util.toStringWithArray(this)
}

case class Tx(  
  ts:Long,          // timestamp
  b:Long,           // block_number
  hash:String,      // transaction_hash
  sz:Int,           // size

  from:String,      // from_address
  to:String,        // to address
  v: BigInt,        // value
  nonce:String,     // nonce
  
  gas:BigInt,       // gas 
  pric:Long,        // gas_coefficient
  
  data:String,      // calldata

  exp:Long,             // expiration
  del:Option[String],   // delegator
  dep:Option[String],   // dependsOn

  used: Long,       // gas_used
  pay: String,      // gas_payer
  paid: BigInt,     // gas_payed
  rwd: BigInt,      // reward
  st: Int,          // status (0 - reverted , 1 - ok)
  
  logs: Array[EventTx],   // Event logs
    
  i:Option[Long] = None,  // transaction index in Block
) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
  override def toString = Util.toStringWithArray(this)
}
