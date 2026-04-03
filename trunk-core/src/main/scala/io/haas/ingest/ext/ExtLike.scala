package io.haas.ingest.ext

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

trait BlockLike {  
  // type Self <: BlockLike
  def number:Long
  //def getTx():Option[Seq[TxLike[Self]]]
  def emptyBlock():BlockLike                  // get empty block from Tx  
} 

trait TxLike {
  // type Self <: TxLike

  def sim:Option[String] // simulation status (if available)
  def hash:String     // tx hash
  def timestamp:Long  // tx timestamp
  def index:Long      // tx index in block
  def block_number:Long      // block number
  def transaction_count:Long   // number of transactions in block

  def getBlock[B <: BlockLike]():Option[B]       // get block from Tx   
  def emptyBlock[B <: BlockLike]():Option[B]      // empty block from Tx   
} 
