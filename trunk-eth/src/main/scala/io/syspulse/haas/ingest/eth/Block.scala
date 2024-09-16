package io.syspulse.haas.ingest.eth

import io.syspulse.skel.Ingestable

case class Block(
  i:Long,       // block number
  hash:String,  // block hash
  phash:String, // parent hash
  non:String,   // noce
  uncl:String,  // uncles
  bloom:String, // bloom filter

  txrt:String, // tx root
  strt:String, // state root
  rert:String, // receipt root

  miner:String, // miner
  dif:BigInt,  // difficulty
  dif0:BigInt, // total difficulty
  
  sz:Long,     // size
  data:String, // extra data
  used:Long,   // gas used
  gas:Long,  // gas limit

  ts:Long,  // timestamp
  txn:Long, // transaction count
  fee:Option[Long], // base fee

) extends Ingestable {
  override def getKey:Option[Any] = Some(i)
}
