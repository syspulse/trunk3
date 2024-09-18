package io.syspulse.haas.ingest.eth.flow.rpc3

import java.util.concurrent.atomic.AtomicLong
import io.syspulse.skel.ingest.flow.Flows

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

class ReorgBlock1(depth:Int = 10) extends ReorgBlock(depth) {
  
  def range(cursor:Long,lastBlock:Long) = {
    (cursor - depth) to lastBlock
  }

  // track last block
  // def track(lastBlock:String) = {
  //   val (blockNum,blockHash,ts,txCount) = parseBlock(lastBlock)
    
  //   // check if reorg
  //   val rr = isReorg(blockNum,blockHash)
    
  //   if(rr.size > 0) {
      
  //     log.warn(s"Reorg: block: >>>>>>>>> ${blockNum}/${blockHash}: reorgs=${rr}")
  //     os.write.append(os.Path("REORG",os.pwd),s"${ts},${blockNum},${blockHash},${txCount}}")
  //     reorg(rr)
  //     (true,true)
      
  //   } else {
      
  //     val fresh = cache(blockNum,blockHash,ts,txCount)
  //     (fresh,false)
  //   }
  // }
  
}

