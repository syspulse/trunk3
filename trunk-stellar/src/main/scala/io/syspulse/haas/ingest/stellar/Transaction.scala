package io.syspulse.haas.ingest.stellar

import io.syspulse.skel.Ingestable

case class Operation(
  i:Long,           // operation index
  typ:String,       // type
  sts:String,       // status  
  addr:String,      // account addr
  v:String,         // value (String ??)
  curr:String,      // currency
  dec:Int,          // decimals
)

case class Transaction(  
  ts:Long,              // timestamp
  hash:String,          // transaction hash
  blk:Long,             // block number
  sts:Int,              // status 0 - success
  
  from:String,
  // to:Option[String],  // there is no To in raw transaction
  fee:BigInt,
  //v:BigInt,           // there is no Value in raw transaction

  fromseq: String,      // source_account_sequence
  feeaddr: String,      // fee_account
  feemax: BigInt,       // max_fee

  ops: Int,             // number of operations

  inp: String,          // envelope XDR
  res: String,          // result XDR
  meta: String,         // result_meta XDR
  feemeta: String,      // fee meta XDR

  mtype: String,        // memo type
  sig: Seq[String],     // signatures
  after: Option[Long],          // valid after
  //pre,                  // preconditions
    
  i:Option[Long] = None,// transaction index in Block

) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
}
