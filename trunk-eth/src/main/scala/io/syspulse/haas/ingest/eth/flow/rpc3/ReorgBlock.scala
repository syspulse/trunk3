package io.syspulse.haas.ingest.eth.flow.rpc3

import java.util.concurrent.atomic.AtomicLong
import io.syspulse.skel.ingest.flow.Flows

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

case class CachedBlock(num:Long,hash:String,ts:Long = 0L,txCound:Long = 0)

abstract class ReorgBlock(val depth:Int = 10) {
  protected val log = Logger(this.getClass())  
  override def toString() = s"${last}"
  
  var last:List[CachedBlock] = List() // hashes of last blocks to detect reorg 

  def isReorg(block:Long,blockHash:String):List[CachedBlock] = {
    val reorgs =  {
      // println(s"============> ${block} (${blockHash}): ${LastBlock.lastBlock}")
      if(last.size == 0)
        return List.empty

      // check for the same block repeated: if current==last and hashes are the same it is not reorg
      if(last.head.num == block && last.head.hash == blockHash) 
        return List.empty
                      
      // find reorg-ed block
      // Due to lag repeats this list may return List() for repeats !
      val blockIndex = last.zipWithIndex.find{ case(b,i) =>
        b.num == block && b.hash != blockHash            
      }
      val reorgs = blockIndex match {
        case Some(bi) => last.take(bi._2 + 1)
        case None => List()
      }
      
      if(blockIndex.isDefined) {
        //log.warn(s"reorg block: ${block}/${blockHash}: reorgs=${reorgs}")
      }

      reorgs
    }
    reorgs
  }

  def reorg(blocks:List[CachedBlock]):List[CachedBlock] = {
    if(blocks.size == 0) 
      return List.empty

    log.info(s"reorg: reorg=(blocks=${blocks},last=${last})")
    last = last.toSet.&~(blocks.toSet).toList
    last      
  }

  def cache(block:Long,blockHash:String,ts:Long = 0L, txCount:Long = 0):Boolean = {    
    log.debug(s"reorg: cache=${block},last=${last})")
    
    if(last.size != 0 && last.find(_.hash == blockHash).isDefined) {      
      // don't add the same blocks, because of how reorging works it will create duplicates 
      false

    } else {
      last = last.+:(CachedBlock(block,blockHash,ts,txCount))
      
      if(last.size > depth) {
        last = last.take(depth)
      }
      true
    }
  }    
  

  def parseBlock(lastBlock:String)= {    
    val r = ujson.read(lastBlock)
    
    // we support block and head parsing    
    if(r.obj.contains("result")) {
      val result = r.obj("result").obj
      
      val blockNum = java.lang.Long.decode(result("number").str).toLong
      val blockHash = result("hash").str
      val ts = java.lang.Long.decode(result("timestamp").str).toLong
      val txCount = result("transactions").arr.size
      
      (blockNum,blockHash,ts,txCount)

    } else {
      val result = r.obj("params").obj("result").obj

      val blockNum = java.lang.Long.decode(result("number").str).toLong
      val blockHash = result("hash").str
      val ts = java.lang.Long.decode(result("timestamp").str).toLong      
      
      (blockNum,blockHash,ts,0)
    }
    
  }

  // track
  def range(cursor:Long,lastBlock:Long):scala.collection.immutable.NumericRange.Inclusive[Long]

  // track last block
  def track(lastBlock:String):(Boolean,Boolean)
}

