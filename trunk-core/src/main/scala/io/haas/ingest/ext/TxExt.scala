package io.haas.ingest.ext

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

// Ext Tx (used as universal Tx for all blockchains)
// Currently is ETL based for compatibility with ethereum-etl / ext legacy
case class TxExt(
  hash:String,
  
  nonce:BigInt,
  
  transaction_index:Int,
  from_address:String,
  to_address:Option[String],
  value:BigInt,
  gas:Long,
  gas_price:Option[BigInt],
  input:String,
  max_fee_per_gas:Option[BigInt],
  max_priority_fee_per_gas:Option[BigInt],
  transaction_type:Option[Int],

  receipt_cumulative_gas_used:Long,
  receipt_gas_used:Long,
  receipt_contract_address:Option[String],
  receipt_root:Option[String],
  receipt_status:Option[Int],
  receipt_effective_gas_price:Option[BigInt],

  block:BlockExt,
  logs:Array[LogExt],
  
  sim:Option[String] = None, // sim
  
) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)

  // override to show Array in a nice way
  override def toString() = Util.toStringWithArray(this)
}
