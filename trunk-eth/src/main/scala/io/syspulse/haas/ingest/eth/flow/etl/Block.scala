package io.syspulse.haas.ingest.eth.etl

import io.syspulse.skel.Ingestable

case class Block(  
  number:Long,
  hash:String,
  parent_hash:String,
  nonce:String,
  sha3_uncles:String,
  logs_bloom:String,
  transactions_root:String,
  state_root:String,
  receipts_root:String,
  miner:String,
  difficulty:BigInt,
  total_difficulty:BigInt,
  size:Long,
  extra_data:String,
  gas_limit:Long,
  gas_used:Long,
  timestamp:Long,
  transaction_count:Long,
  base_fee_per_gas:Option[Long],

) extends Ingestable {
  override def getKey:Option[Any] = Some(number)
}
