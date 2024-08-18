package io.syspulse.haas.ingest.stellar

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

// case class Operation(
//   i:Long,           // operation index
//   typ:String,       // type
//   st:String,       // status  
//   addr:String,      // account addr
//   v:String,         // value (String ??)
//   curr:String,      // currency
//   dec:Int,          // decimals
// )

// case class Tx(  
//   ts:Long,              // timestamp
//   hash:String,          // transaction hash
//   b:Long,               // block number
//   st:Int,               // status 0 - success
  
//   from:String,
//   to:Option[String],  
//   fee:BigInt,
//   v:BigInt,             // there is no Value in raw transaction

//   typ:String,           // operation type

//   fromseq: String,      // source_account_sequence
//   feeaddr: String,      // fee_account
//   feemax: BigInt,       // max_fee

//   ops: Int,             // number of operations
  
//   mtype: String,        // memo type
//   sig: Array[String],     // signatures
//   after: Option[Long],    // valid after
//   //pre,                  // preconditions
    
//   i:Option[Long] = None,// transaction index in Block

// ) extends Ingestable {
//   override def getKey:Option[Any] = Some(hash)
//   override def toString = Util.toStringWithArray(this)
// }

case class Tx(  
  ts:Long,              // timestamp
  b:Long,               // block number
  hash:String,          // transaction hash  
  st:Int,               // status 0 - success
  
  from:String,
  to:Option[String],    
  v:BigInt,             // there is no Value in raw transaction
  fee:BigInt,

  typ:String,           // operation type
 
) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
  override def toString = Util.toStringWithArray(this)
}
