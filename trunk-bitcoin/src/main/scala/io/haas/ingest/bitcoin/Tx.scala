package io.haas.ingest.bitcoin

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util
import io.haas.ingest.ext.TxLike

case class Tx(  
  ts:Long,          // timestamp (millisec)
  txid:String,      // txid 
  hash:String,      // transaction hash
  
  from:String,      // from address ("COINBASE" is sued fro Block Reward transaction)
  to:String,        // to address
  v:BigInt,         // value (always in sats)
  fee:Option[BigInt], // fee (always in sats)

  ver: Int,                // Version number
  sz: Int,                 // Size in bytes
  vsz: Int,                // Virtual size
  w: Int,                  // Weight
  ts_l: Long,              // Lock time

  block:Block, 
  
  i:Option[Long] = None,  // transaction index in Block

) extends Ingestable with TxLike {
  override def getKey:Option[Any] = Some(hash)
  
  override def timestamp:Long = ts
  override def index:Long = i.getOrElse(0L)
  override def block_number:Long = block.i
  override def transaction_count:Long = block.n
  override def sim:Option[String] = None
  override def emptyBlock[B](): B = {
    block.copy(tx = Some(Array.empty[Transaction])).asInstanceOf[B]
  }

  override def toString = Util.toStringWithArray(this)
}
