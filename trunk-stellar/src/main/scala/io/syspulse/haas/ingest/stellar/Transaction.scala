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
  
  from:String,
  to:Option[String],
  fee:BigInt,
  v:BigInt,
    
  i:Option[Long] = None,// transaction index in Block

) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
}
