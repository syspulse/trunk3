package io.syspulse.haas.ingest.eth

import scala.jdk.CollectionConverters._

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

import spray.json._

case class EvmTx(
  ts:Long,
  pool:String, // queued, pending
  blockHash:Option[String],
  blockNumber:Option[Long],
  from: String,
  gas: Long,
  gasPrice: BigInt,
  maxFeePerGas: Option[BigInt],
  maxPriorityFeePerGas: Option[BigInt],
  hash: String,
  input: String,
  nonce: Long,
  to: Option[String],
  transactionIndex: Option[Int],
  value: BigInt,
  `type`: Byte,
  accessList: Option[List[EvmAccessList]],
  chainId: Option[Int],
  v: Byte,
  r: String,
  s: String
) extends Ingestable {
  import EvmTxPoolJson._
  // default is json interchange format
  override def toLog = this.toJson.compactPrint
}

case class EvmAccessList(address:String,storageKeys:List[String])

case class EvmTxRaw(
  blockHash:Option[String],
  blockNumber:Option[Long],
  from: String, 
  gas: String,
  gasPrice: String,
  maxFeePerGas: Option[String],
  maxPriorityFeePerGas: Option[String],
  hash: String,
  input: String,
  nonce: String,
  to: Option[String],
  transactionIndex: Option[Int],
  value: String,
  `type`: String,
  accessList: Option[List[EvmAccessList]],
  chainId: Option[String],
  v: String,
  r: String,
  s: String

) {
  
  def unraw(ts:Long,pool:String):EvmTx = EvmTx(
      ts,
      pool,
      this.blockHash,
      this.blockNumber,
      this.from: String,
      java.lang.Long.parseLong(this.gas.drop(2),16),
      BigInt(Util.unhex(gasPrice)),
      maxFeePerGas.map(v => BigInt(Util.unhex(v))),
      maxPriorityFeePerGas.map(v => BigInt(Util.unhex(v))),
      this.hash,
      this.input,
      Integer.parseInt(nonce.drop(2),16),
      this.to,
      this.transactionIndex,
      BigInt(Util.unhex(value)),
      Integer.parseInt(`type`.drop(2),16).toByte,
      this.accessList,
      chainId.map(v => Integer.parseInt(v.drop(2),16)),
      Integer.parseInt(v.drop(2),16).toByte,
      this.r,
      this.s
  )
}

case class EvmTxPoolResult(pending:Map[String,Map[String,EvmTxRaw]],queued:Map[String,Map[String,EvmTxRaw]])

case class EvmTxPool(
  jsonrpc:String,
  id:Long,
  result:EvmTxPoolResult
)
