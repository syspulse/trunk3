package io.syspulse.haas.ingest.eth.etl

import io.syspulse.skel.Ingestable

// Fat Tx from ethereum-etl
case class Tx(
  hash:String,
  nonce:Long,
  transaction_index:Int,
  from_address:String,
  to_address:Option[String],
  value:BigInt,
  gas:Long,
  gas_price:BigInt,
  input:String,
  max_fee_per_gas:Option[BigInt],
  max_priority_fee_per_gas:Option[BigInt],
  transaction_type:Option[Int],

  cumulative_gas_used:Long,
  gas_used:Long,
  contract_address:Option[String],
  root:Option[String],
  status:Option[Int],
  effective_gas_price:Option[BigInt],

  block:Block,
  logs:Seq[LogTx]
  
) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
}
