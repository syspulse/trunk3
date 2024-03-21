package io.syspulse.haas.ingest.icp

import io.syspulse.skel.Ingestable


// case class Operation(
//   i:Long,           // operation index
//   typ:String,       // type
//   st:String,       // status  
//   addr:String,      // account addr
//   v:String,         // value (String ??)
//   curr:String,      // currency
//   dec:Int,          // decimals
// )

case class Transaction(  
  ts:Long,              // timestamp
  hash:String,          // transaction hash
  blk:Long,             // block number
  
  from:Option[String],
  to:Option[String],
  fee:BigInt,
  v:BigInt,
  
  alw:Option[BigInt],   // allowance
  alwe:Option[BigInt],  // expected allowence

  spend:Option[String], // spender
  
  typ:String,           // transfer type
  memo:String,          // 
  icrc1:Option[String], // icrc1 memo
  
  exp:Option[Long],     // expiration  
  
  i:Option[Long] = None,// transaction index in Block

) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
}
