package io.syspulse.haas.ingest.starknet

import io.syspulse.skel.Ingestable

case class Transaction(  
  hash:String,      // transaction hash
  nonce:Long,
  from:String,
  fee:Option[BigInt],
  typ:String,
  ver:Int,
  sig:String,       // signature (can be empty)
  
  data:Seq[String], // calldata
  
  entry:Option[String],     // entry_point_selector

  b:Long,           // block number
  ts:Long,          // timestamp
  
  i:Option[Long] = None,  // transaction index in Block

) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
}
