package io.haas.ingest.ext

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util


// Ext Tx (used as universal Tx for all blockchains)
// Currently is ETL based for compatibility with ethereum-etl / ext legacy
case class Tx(
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

  block:Block,
  logs:Array[Log],
  
  sim:Option[String] = None, // sim
  
) extends TxLike with Ingestable {
  override def getKey:Option[Any] = Some(hash)
  override def timestamp:Long = block.timestamp
  override def index:Long = transaction_index.toLong
  override def block_number:Long = block.number
  override def transaction_count:Long = block.transaction_count

  // override to show Array in a nice way
  override def toString() = Util.toStringWithArray(this)

  override def emptyBlock[B](): B = {
    Block(
      number = block.number,
      hash = block.hash,
      parent_hash = block.parent_hash,
      nonce = block.nonce,
      sha3_uncles = None,
      logs_bloom = "",
      transactions_root = block.transactions_root,
      state_root = block.state_root,
      receipts_root = block.receipts_root,
      miner = block.miner,
      difficulty = block.difficulty,
      total_difficulty = block.total_difficulty,
      size = block.size,
      extra_data = block.extra_data,
      gas_limit = block.gas_limit,

      gas_used = block.gas_used,
      timestamp = block.timestamp,
      transaction_count = block.transaction_count,
      base_fee_per_gas = block.base_fee_per_gas
    ).asInstanceOf[B]
  }
}
