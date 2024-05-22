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
  b:Long,           // block number
  hash:String,      // transaction hash
  sz:Int,           // size

  from:String,      // from address
  to:String,        // to address
  v: BigInt,        // value
  nonce:String,     // nonce
  
  gas:BigInt,       // gas 
  pric:Long,         // gas coefficient
  
  data:String,      // calldata

  exp:Long,         // expirateion
  del:Option[String],       // delegator
  dep:Option[String],       // dependsOn

  used: Long,       // gas used
  pay: String,      // gas payer
  paid: BigInt,     // gas payed
  rwd: BigInt,      // reward
  st: Int,          // status (0 - reverted , 1 - ok)
  
  logs: Array[EventTx],   // Event logs
    
  i:Option[Long] = None,  // transaction index in Block
) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
  override def toString = Util.toStringWithArray(this)
}
