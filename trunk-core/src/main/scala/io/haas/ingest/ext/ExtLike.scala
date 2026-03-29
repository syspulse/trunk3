package io.haas.ingest.ext

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

trait BlockLike {  
  def number:Long
} 

trait TxLike {
  def sim:Option[String] // simulation status (if available)
  def hash:String     // tx hash
  def timestamp:Long  // tx timestamp
  def index:Long      // tx index in block
  def block_number:Long      // block number
  def transaction_count:Long   // number of transactions in block

  def emptyBlock[B]():B
} 
