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
  hash:String,          // transaction_hash
  blk:Long,             // block_number
  
  from:Option[String], // from
  to:Option[String],   // to
  fee:BigInt,          // fee
  v:BigInt,            // value
  
  alw:Option[BigInt],   // allowance
  alwe:Option[BigInt],  // allowance_expected

  spend:Option[String], // spender
  
  typ:String,           // transfer_type
  memo:String,          // 
  icrc1:Option[String], // icrc1_memo
  
  exp:Option[Long],     // expiration  
  
  i:Option[Long] = None,// transaction_index

) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
}
