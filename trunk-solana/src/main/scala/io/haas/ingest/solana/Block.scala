package io.haas.ingest.solana

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util
import io.haas.ingest.ext.{BlockLike,TxLike}
import scala.collection.immutable.ArraySeq

case class Block(
  ts:Long,      // timestamp
  b:Long,       // block number (parent slot - 1)    
  h:Long,       // block height

  hash:String,  // block hash 
  phash:String, // parent hash  

  tx:Option[Array[Tx]], // transactions  

) extends Ingestable with BlockLike {
  override def getKey:Option[Any] = Some(b)
  override def number:Long = b
  //override def getTx():Option[Seq[Tx]] = tx.map(ArraySeq.unsafeWrapArray(_))
  override def emptyBlock() = this.copy(tx = None)

  override def toString = Util.toStringWithArray(this)
}
