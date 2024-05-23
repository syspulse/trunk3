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

case class Transaction(  
  ts:Long,              // timestamp
  b:Long,               // block_number
  hash:String,          // transaction_hash
  st:Int,               // status (0 - success, 1 - fail)
  
  from:String,          // from
  // to:Option[String],  // there is no To in raw transaction
  fee:BigInt,           // fee
  //v:BigInt,           // value there is no Value in raw transaction

  fromseq: String,      // source_account_sequence
  feeaddr: String,      // fee_account
  feemax: BigInt,       // max_fee

  ops: Int,             // operations_number

  inp: String,          // input (envelope XDR)
  res: String,          // result (result XDR)
  meta: String,         // result_meta (result meta XDR)
  feemeta: String,      // fee_meta (fee meta XDR)

  mtype: String,        // memo_type
  sig: Array[String],   // signatures
  after: Option[Long],  // valid_after
  //pre,                // preconditions
    
  i:Option[Long] = None,// transaction index in Block

) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
  override def toString = Util.toStringWithArray(this)
}
