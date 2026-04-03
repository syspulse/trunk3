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
  //override type Self = Tx
  override def getKey:Option[Any] = Some(hash)
  override def timestamp:Long = block.timestamp
  override def index:Long = transaction_index.toLong
  override def block_number:Long = block.number
  override def transaction_count:Long = block.transaction_count

  override def getBlock[B <: BlockLike]():Option[B] = Some(block.asInstanceOf[B])
  override def emptyBlock[B <: BlockLike]():Option[B] = Some(block.emptyBlock().asInstanceOf[B])
  
  // override to show Array in a nice way
  override def toString() = Util.toStringWithArray(this)
  
}
