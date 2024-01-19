package io.syspulse.haas.ingest.eth.flow.rpc

import java.util.concurrent.atomic.AtomicLong
import io.syspulse.skel.ingest.flow.Flows

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

case class Block(num:Long,hash:String,ts:Long = 0L,txCound:Long = 0)

case class LastBlockState(
  next:Long,       // next EXPECTED block !
  blockStart:Long, 
  blockEnd:Long, 
  stateStore:Option[String], 
  lag:Int = 0,               // lag means how many blocks wait until moving to the next block
                             // this is also a depth of last history to check for reorg 
  last:List[Block] = List(), // hashes of last blocks to detect reorg
  blockReorg:Option[Long] = None,
  var lagging:Int = 0         // current lag counter (from lag to 0). On lagging == 0 next is incremented
)

object LastBlock {  
  @volatile
  private var lastBlock: Option[LastBlockState] = None 
}

class LastBlock {
  private val log = Logger(s"LastBlock")
  
  override def toString() = s"${LastBlock.lastBlock}"

  def isBehind(block:Long):Long = {
    LastBlock.lastBlock.synchronized {
      LastBlock.lastBlock match {
        case Some(lb) =>
          if(block > lb.next) {
            val behind = block - lb.next
            behind
          } else
            0 
        case None => 
          0
      }   
    }
  }

  def isReorg(block:Long,blockHash:String):List[Block] = {
    val reorgs = LastBlock.lastBlock.synchronized {      
      LastBlock.lastBlock match {
        case Some(lb) =>
          
          // println(s"============> ${block} (${blockHash}): ${LastBlock.lastBlock}")
          if(lb.last.size == 0)
            return List.empty

          // check for the same block repeated: if current==last and hashes are the same it is not reorg
          if(lb.last.head.num == block && lb.last.head.hash == blockHash) 
            return List.empty

          if(block > lb.next) {
            log.warn(s"non-sequential blocks: next=${lb.next}, new=${block}: Reduce RCP query interval")
            return List.empty
          }

          // if next block, no re-org
          // if(lb.next == block)
          //   return List.empty
                    
          // find reorg-ed block
          // Due to lag repeats this list may return List() for repeats !
          val blockIndex = lb.last.zipWithIndex.find{ case(b,i) =>
            b.num == block && b.hash != blockHash            
          }
          val reorgs = blockIndex match {
            case Some(bi) => lb.last.take(bi._2 + 1)
            case None => List()
          }
          
          if(blockIndex.isDefined) {
            log.warn(s"reorg block: ${block}/${blockHash}: next=${lb.next}, reorgs=${reorgs}")
          }

          reorgs
          
        case None => 
          List.empty
      }
    }

    reorgs
  }

  def reorg(blocks:List[Block]):List[Block] = {
    if(blocks.size == 0) 
      return List.empty

    LastBlock.lastBlock.synchronized {      
      LastBlock.lastBlock match {
        case Some(lb) =>
          // infrequent operation, so safe to "toSet"
          log.info(s"reorg: reorg=(blocks=${blocks},last=${lb.last})")
          LastBlock.lastBlock = Some(lb.copy(last = lb.last.toSet.&~(blocks.toSet).toList))
          LastBlock.lastBlock.get.last
        case None => 
          List.empty
      }
    }    
  }

  // returns true if alreay committed and moves the counter 
  // ATTENTION: does not check for Reorg !
  def commit(block:Long,blockHash:String,ts:Long = 0L, txCount:Long = 0):Boolean = {
    LastBlock.lastBlock.synchronized {      
      LastBlock.lastBlock.map(lb => {
        
        val committedBlock = lb.last.headOption match {
          case Some(b) => b.num
          case None => -1
        }

        if(committedBlock != block) {
          
          log.info(s"COMMIT: ${block}/${blockHash}")
          val last = 
            if(lb.last.size > lb.lag)
              lb.last.take(lb.lag)
            else
              lb.last

          LastBlock.lastBlock = Some(lb.copy(
            //next = block + (if(lb.lagging > 0) 1 else 0), 
            next = block + 1,
            last = last.+:(Block(block,blockHash,ts,txCount)),
            lagging = 0
          ))

          false

        } else {          
          log.info(s"COMMIT: already: ${block}/${blockHash}")

          LastBlock.lastBlock = Some(lb.copy(
            // next = block + (if(lb.lagging == 1) 1 else 0),
            lagging = lb.lagging + 1
          ))
          true
        }
                
      })
    }.getOrElse(false)
  }

  def isDefined = LastBlock.lastBlock.isDefined

  def set(next:Long,blockStart:Long,blockEnd:Long = Long.MaxValue,stateFile:Option[String] = None, lag:Int = 0) = {
    LastBlock.lastBlock = LastBlock.lastBlock.synchronized {
      LastBlock.lastBlock match {
        case Some(_) => LastBlock.lastBlock
        case None => Some(LastBlockState(next,blockStart,blockEnd,stateFile, lag = lag))    
      }
    }    
  }

  def next() = LastBlock.lastBlock.synchronized {
    LastBlock.lastBlock match {
      case Some(lb) => lb.next
      case None => -1
    }
  }

  def committed() = LastBlock.lastBlock.synchronized {
    LastBlock.lastBlock match {
      case Some(lb) => lb.last.headOption.map(_.num).getOrElse(-1)
      case None => -1
    }
  }

  def end() = LastBlock.lastBlock.synchronized {
    LastBlock.lastBlock match {
      case Some(lb) => lb.blockEnd
      case None => -1
    }
  }

  // size can be lag + 1 maximum
  def size() = LastBlock.lastBlock.synchronized {
    LastBlock.lastBlock match {
      case Some(lb) => lb.last.size
      case None => 0
    }
  }

  def last() = LastBlock.lastBlock.synchronized {
    LastBlock.lastBlock match {
      case Some(lb) => lb.last
      case None => List.empty
    }
  }

  def reset() = LastBlock.lastBlock = None
}

