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
  st:Int,               // status
  
  from:String,          // from
  // to:Option[String],  // there is no To in raw transaction
  fee:BigInt,           // fee
  //v:BigInt,           // value there is no Value in raw transaction

  fromseq: String,      // source_account_sequence
  feeaddr: String,      // fee_account
  feemax: BigInt,       // max_fee

  ops: Int,             // operations_number

  inp: String,          // input_xdr
  res: String,          // result_xdr
  meta: String,         // result_meta_xdr
  feemeta: String,      // fee_meta_xdr

  mtype: String,        // memo_type
  sig: Array[String],   // signatures
  after: Option[Long],  // valid_after
  //pre,                // preconditions
    
  i:Option[Long] = None,// transaction_index

) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
  override def toString = Util.toStringWithArray(this)
}
