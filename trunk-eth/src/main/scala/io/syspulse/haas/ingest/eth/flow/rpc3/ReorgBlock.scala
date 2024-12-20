package io.syspulse.haas.ingest.eth.flow.rpc3

import java.util.concurrent.atomic.AtomicLong
import io.syspulse.skel.ingest.flow.Flows

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

case class CachedBlock(num:Long,hash:String,ts:Long = 0L,txCount:Long = 0)

abstract class ReorgBlock(val depth:Int,reorgFile:String) {
  protected val log = Logger(this.getClass())  
  override def toString() = s"${last}"
  
  var last:List[CachedBlock] = List() // hashes of last blocks to detect reorg 

  def isReorg(block:Long,blockHash:String):List[CachedBlock] = {
    //log.warn(s"isReorg: block=${block}: last=${last})")

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

    log.warn(s"reorg: blocks=${blocks},last=${last}")
    last = last.toSet.&~(blocks.toSet).toList
    last      
  }

  def cache(block:Long,blockHash:String,ts:Long = 0L, txCount:Long = 0):Boolean = {        
    
    if(last.size != 0 && last.find(b => b.num == block && b.hash == blockHash).isDefined) {      
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
  def track(lastBlock:String):(Boolean,List[CachedBlock]) = {
    val (blockNum,blockHash,ts,txCount) = parseBlock(lastBlock)
    
    track(blockNum,blockHash,ts,txCount)
  }

  def track(blockNum:Long,blockHash:String,ts:Long,txCount:Long):(Boolean,List[CachedBlock]) = {
    
    // check if reorg
    val rr = isReorg(blockNum,blockHash)
    
    if(rr.size > 0) {
      
      log.warn(s"Reorg: block: >>>>>>>>> ${blockNum}/${blockHash}: reorgs=${rr}")

      if(reorgFile.nonEmpty) {
        try {        
          os.write.append(os.Path(reorgFile),s"${ts},${blockNum},${blockHash},${txCount}\n")          
        } catch {
          case e:Exception => log.error(s"Failed to write reorg to file: '${reorgFile}': ${e}")
        }
      }

      // !
      reorg(rr)
      
      (true,rr)      
    } else {
      
      val fresh = cache(blockNum,blockHash,ts,txCount)      
      (fresh,List.empty)
    }
  }
}

