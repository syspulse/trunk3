package io.syspulse.haas.ingest.eth

import com.typesafe.scalalogging.Logger

// ATTENTION
import io.syspulse.skel.service.JsonCommon
import io.syspulse.skel.Ingestable

import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

case class EthBlock(
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
)  extends Ingestable

case class EthTransaction(
  block_timestamp:Long,
  transaction_index:Int,
  hash:String,
  block_number:Long,
  from_address:String,
  to_address:Option[String],
  gas:Long,
  gas_price:BigInt,
  input:String,
  value:BigInt,

  nonce:Long,
  max_fee_per_gas:Option[BigInt] = None,
  max_priority_fee_per_gas:Option[BigInt] = None, 
  transaction_type: Option[Int], 
  receipt_cumulative_gas_used: Long, 
  receipt_gas_used: Long, 
  receipt_contract_address: Option[String], 
  receipt_root: Option[String], 
  receipt_status: Option[Int], 
  receipt_effective_gas_price: Option[BigInt]

)  extends Ingestable

case class EthBlockTx(
  number:Long,
  hash: String,
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
)  extends Ingestable

case class EthLogTx(
  index:Int,
  address:String,
  data:String,
  topics: Array[String], 
) extends Ingestable


case class EthTx(
  // block_timestamp:Long,
  transaction_index:Int,
  hash:String,
  // block_hash:String,
  // block_number:Long,
  from_address:String,
  to_address:Option[String],
  gas:Long,
  gas_price:BigInt,
  input:String,
  value:BigInt,

  nonce:Long,
  max_fee_per_gas:Option[BigInt] = None,
  max_priority_fee_per_gas:Option[BigInt] = None, 
  transaction_type: Option[Int], 
  cumulative_gas_used: Long, 
  gas_used: Long, 
  contract_address: Option[String], 
  root: Option[String], 
  status: Option[Int], 
  effective_gas_price: Option[BigInt],

  block:EthBlockTx,
  logs:Array[EthLogTx]

)  extends Ingestable


case class EthTokenTransfer(
  tokenAddress:String,
  from:String,
  to:String,
  value:BigInt,
  txHash:String,
  logIndex:Int,
  blockNumber:Long,
  blockTimestamp:Long) extends Ingestable

case class EthLog(
  log_index:Int,
  transaction_hash:String,
  transaction_index:Int,
  address:String,
  data:String,
  topics: Array[String], 
  block_number: Long, 
  block_timestamp:Long, 
  block_hash:String) extends Ingestable

// ATTENTION: This is format of ethereum-etl but converges into our internal format !

object EthEtlJson extends JsonCommon with NullOptions with ProductFormatsInstances {

  import DefaultJsonProtocol._
  implicit val jf_etl_transaction = jsonFormat20(EthTransaction) //jsonFormat(EthTx,"block_timestamp","transaction_index","hash","block_number","from_address","to_address","gas","gas_price","input","value")    
  implicit val jf_etl_bl = jsonFormat(EthBlock,
  "number","hash","parent_hash","nonce",
  "sha3_uncles","logs_bloom","transactions_root", "state_root", "receipts_root", "miner", "difficulty", "total_difficulty", "size", "extra_data", "gas_limit", "gas_used", "timestamp",
  "transaction_count","base_fee_per_gas")
  implicit val jf_etl_tt = jsonFormat(EthTokenTransfer,"token_address","from_address","to_address","value","transaction_hash","log_index","block_number","block_timestamp")
  implicit val jf_etl_lo = jsonFormat(EthLog,"log_index","transaction_hash","transaction_index","address","data","topics","block_number","block_timestamp","block_hash")

  implicit val jf_etl_tx_block = jsonFormat19(EthBlockTx)
  implicit val jf_etl_tx_log = jsonFormat4(EthLogTx)
  implicit val jf_etl_tx = jsonFormat(EthTx,
    "transaction_index","hash","from_address","to_address","gas","gas_price","input","value",
    "nonce","max_fee_per_gas","max_priority_fee_per_gas","transaction_type","cumulative_gas_used","gas_used",
    "contract_address","root","status","effective_gas_price","block","logs"
  )    

}
