package io.syspulse.haas.ingest.eth

import io.syspulse.skel.Ingestable

// ETL type flat transaction
case class Transaction(
  ts:Long,
  i:Int,
  hash:String,
  blk:Long,
  from:String,
  to:Option[String],
  gas:Long,
  p:BigInt,
  inp:String,
  v:BigInt,

  non:Long,
  fee:Option[BigInt],
  tip:Option[BigInt], 
  typ:Option[Int],
  used2: Long,            // cumulative used
  used: Long,             // gas used
  con: Option[String],  // contract
  root: Option[String],   // receipt root
  sta: Option[Int],       // status
  p0: Option[BigInt]      // price Effective

  //timestamp:Option[Long]
) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
}
