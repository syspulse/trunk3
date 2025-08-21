package io.syspulse.haas.ingest.vechain

import io.syspulse.skel.Ingestable

case class Transaction(  
  ts:Long,          // timestamp
  b:Long,           // block number
  hash:String,      // transaction hash
  sz:Int,           // size

  from:String,
  to:String,        
  v: BigInt,        // value
  nonce:String,     // nonce
  
  gas:BigInt,         // gas 
  pric:Option[Long],  // gas coefficient
  
  data:String,      // calldata

  blk:String,       // blockref
  exp:Long,          // expirateion
  del:Option[String],       // delegator
  dep:Option[String],       // dependsOn
    
  i:Option[Long] = None,  // transaction index in Block
) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
}
