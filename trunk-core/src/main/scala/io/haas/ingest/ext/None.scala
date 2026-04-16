package io.haas.ingest.ext

class TxNone extends TxLike {
  //override type Self = TxNone
  override def hash:String = ""
  override def timestamp:Long = 0
  override def index:Long = 0
  override def block_number:Long = 0
  override def transaction_count:Long = 0
  override def sim:Option[String] = None  
  def getBlock[B <: BlockLike]():Option[B] = None  
  def emptyBlock[B <: BlockLike]():Option[B] = None
}

class BlockNone extends BlockLike {
  override def number:Long = 0
  //override def getTx() = None

  override def emptyBlock() = {
    Block(
        number = 0, 
        hash = "", 
        parent_hash = "", 
        nonce = None, 
        sha3_uncles = None, 
        logs_bloom = "", 
        transactions_root = "", 
        state_root = "", 
        receipts_root = "", 
        miner = "", 
        difficulty = BigInt(0), 
        total_difficulty = None, 
        size = 0, 
        extra_data = "", 
        gas_limit = 0, 
        gas_used = 0, 
        timestamp = 0, 
        transaction_count = 0, 
        base_fee_per_gas = None)
  }
}

class MempoolNone
