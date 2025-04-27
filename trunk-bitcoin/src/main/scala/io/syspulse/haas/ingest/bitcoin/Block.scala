package io.syspulse.haas.ingest.bitcoin

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class Block(
  ts:Long,       // timestamp
  i:Long,       // block number  
  hash:String,  // block hash 
  phash:String, // parent hash  

  c: Int,           // Number of confirmations
  ver: Long,                // Version
  merkle: String,           // Merkle root
  ts_m: Long,            // Median time
  nonce: Long,                 // Nonce
  bits: String,                // Bits
  d: BigInt,          // Mining difficulty
  cwork: String,           // Chain work
  n: Int,                    // Number of transactions
  nhash: Option[String],  // Next block hash
  ssz: Int,          // Stripped size
  sz: Int,                   // Block size
  w: Int,                   // Block weight
  
  tx:Option[Array[Transaction]] = None, // transactions  

) extends Ingestable {
  override def getKey:Option[Any] = Some(i)

  override def toString = Util.toStringWithArray(this)
}
