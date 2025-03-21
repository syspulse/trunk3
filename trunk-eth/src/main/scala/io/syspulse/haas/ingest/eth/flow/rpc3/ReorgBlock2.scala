package io.syspulse.haas.ingest.eth.flow.rpc3

import java.util.concurrent.atomic.AtomicLong
import io.syspulse.skel.ingest.flow.Flows

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

class ReorgBlock2(depth:Int = 10,reorgFile:String = "") extends ReorgBlock(depth,reorgFile) {
  
  def range(cursor:Long,lastBlock:Long) = {    
    cursor to lastBlock
  }

  override def cache(blockNum:Long,blockHash:String,ts:Long,txCount:Long):Boolean = {
    // ignore fresh
    super.cache(blockNum,blockHash,ts,txCount)
    true
  }
}

