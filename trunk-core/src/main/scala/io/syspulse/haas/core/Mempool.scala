package io.syspulse.haas.core

import scala.jdk.CollectionConverters._

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util
import io.syspulse.haas.core.TokenBlockchain

case class MempoolTx(
  ts:Long,
  pool:Int, // pending - 0, queued - 1, 
  from: String,
  gas: Long,
  p: BigInt,
  fee: Option[BigInt], // old pre EIP-1155
  tip: Option[BigInt], // old transactions without tip
  hash: String,
  inp: String,
  non: Long,
  to: Option[String],
  v: BigInt,
  typ: Int,
  sig: String
) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
}


case class MempoolBlock(
  ts:Long,
  `new`:Int,  // count of new Tx
  `old`:Int,  // count of old Tx 
  `out`:Int,  // count of disappeared Tx
) extends Ingestable {

  override def getKey:Option[Any] = Some(ts)
}