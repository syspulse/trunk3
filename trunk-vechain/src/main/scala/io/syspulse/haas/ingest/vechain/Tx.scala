package io.syspulse.haas.ingest.vechain

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class EventTx(
  contract:String,
  data:String,  
  topics:Array[String] = Array(),
) {
  override def toString = Util.toStringWithArray(this)
}

case class Tx(  
  ts:Long,          // timestamp
  b:Long,           // block number
  hash:String,      // transaction hash
  sz:Int,           // size

  from:String,
  to:String,        
  v: BigInt,        // value
  nonce:String,     // nonce
  
  gas:BigInt,         // gas 
  pric:Int,           // gas coefficient
  
  data:String,      // calldata

  exp:Int,          // expirateion
  del:Option[String],       // delegator
  dep:Option[String],       // dependsOn

  used: Long,       // gas used
  pay: String,      // gas payer
  paid: BigInt,     // gas payed
  rwd: BigInt,      // reward
  fail: Boolean,    // reverted 
  
  logs: Array[EventTx],
    
  i:Option[Long] = None,  // transaction index in Block
) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
  override def toString = Util.toStringWithArray(this)
}
