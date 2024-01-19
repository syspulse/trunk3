package io.syspulse.haas.ingest.vechain

import io.syspulse.skel.Ingestable

case class Block(
  ts:Long,      // timestamp
  i:Long,       // block number
  hash:String,  // block hash
  phash:String, // parent hash
  sz:Int,       // size

  gas:Long,         // timestamp
  ben: String,      // beneficiary
  used: Long,       // gas used
  scor: Long,        // totalscore
  troot: String,       // tx root
  feat: Int,          // tx features
  sroot: String,      // state root
  rroot: String,      // receipt root
  com: Boolean,       // com
  signr: String,      // signer
  trnk: Boolean,      // is trunk
  fin: Boolean,       // is finalized
  
  tx:Option[Seq[Transaction]], // transactions
  
) extends Ingestable {
  override def getKey:Option[Any] = Some(i)
}
