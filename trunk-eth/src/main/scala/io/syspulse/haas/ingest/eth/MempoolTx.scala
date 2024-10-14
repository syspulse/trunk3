package io.syspulse.haas.ingest.eth

import scala.jdk.CollectionConverters._

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class MempoolTx(
  ts:Long,
  pool:String,          // NOTE: CHANGE TO Byte: pending - 0, queued - 1,
  bhash:Option[String],         // blockhash
  b:Option[Long],               // blocknumber
  from: String,
  gas: Long,
  p: BigInt,
  fee: Option[BigInt], // old pre EIP-1155
  tip: Option[BigInt], // old transactions without tip
  hash: String,
  inp: Option[String],
  non: BigInt,
  to: Option[String],
  i: Option[Int],              // transaction index
  v: Option[BigInt],
  typ: Int,
  chid: Option[Long],             // chainId
  sig: Option[String],           // zkSync may not have signature

  trace:Option[Array[CallTrace]] = None, // optiona traces

) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
  override def toString() = Util.toStringWithArray(this)
}

case class MempoolBlock(
  ts:Long,
  `new`:Int,  // count of new Tx
  `old`:Int,  // count of old Tx 
  `out`:Int,  // count of disappeared Tx
) 

case class MempoolTransaction(
  ts:Long,
  hash: String,  
) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
  
}