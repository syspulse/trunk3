package io.syspulse.haas.ingest.eth.flow.rpc3

import java.util.concurrent.atomic.AtomicLong
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

import com.github.tototoshi.csv._

import io.syspulse.skel.config._
import io.syspulse.skel.util.Util
import io.syspulse.skel.config._

import io.syspulse.skel.ingest._

import spray.json._
import DefaultJsonProtocol._
import java.util.concurrent.TimeUnit

import io.syspulse.haas.ingest.eth._
import io.syspulse.haas.ingest.eth.EthURI

import io.syspulse.haas.core.RetryException
import io.syspulse.haas.ingest.eth.flow.rpc3._
import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson

import io.syspulse.haas.ingest.Decoder
import io.syspulse.skel.util.DiffSet

trait RPCDecoder[T] extends Decoder[T,RpcBlock,RpcTx,RpcTokenTransfer,RpcLog,RpcTx] {

  protected val log = Logger(s"${this}")

  import EthRpcJson._
  import TxJson._
  import BlockJson._
  import TokenTransferJson._
  import EventJson._
  
  def parseBlock(data:String):Seq[RpcBlock] = {
    if(data.isEmpty()) return Seq()
    
    // only JSON is supported
    if(data.stripLeading().startsWith("{")) {
      
      val block = try {
        data.parseJson.convertTo[RpcBlock]
      } catch {
        case e:Exception => 
          log.error(s"failed to parse: '${data}'",e)
          //log.error(s"failed to parse: '${data}'")
          throw new RetryException(s"failed to parse: '${data}'")          
      }

      if(! block.result.isDefined) {
        log.info(s"block not found: '${data}'")          
        throw new RetryException(s"block not found: '${data.strip}'")
      } 
      
      Seq(block)
      
    } else {
      log.error(s"failed to parse: '${data}'")
      throw new RetryException(s"failed to parse: '${data}'")        
      //Seq.empty
    }        
    
  }

  def parseTransaction(data:String):Seq[RpcTx] = parseTx(data) 

  def parseTx(data:String):Seq[RpcTx] = {
    if(data.isEmpty()) return Seq()
    
      // Only Json from Block is supported
    if(data.stripLeading().startsWith("{")) {
      val block = try {
        data.parseJson.convertTo[RpcBlock]
      } catch {
        case e:Exception => 
          log.error(s"failed to parse: '${data}'",e)
          throw new RetryException(s"failed to parse: '${data}'")          
      }

      if(! block.result.isDefined) {
        log.info(s"block not found: '${data}'")          
        throw new RetryException(s"block not found: '${data.strip}'")
      } 
              
      block.result.get.transactions.toIndexedSeq

    } else {
      log.error(s"failed to parse: '${data}'")
      throw new RetryException(s"failed to parse: '${data}'")        
      //Seq.empty
    }    
  }
  
  def parseTokenTransfer(data:String):Seq[RpcTokenTransfer] = {
    Seq()
  }

  
  def parseEventLog(data:String):Seq[RpcLog] = {
    Seq()
  }

  var pendingDiff = new DiffSet[RpcTxRaw](Set())
  var queuedDiff = new DiffSet[RpcTxRaw](Set())

  def decodeMempoolTx(raw:RpcTxRaw,ts:Long,pool:String):MempoolTx = MempoolTx(
      ts,
      pool,
      raw.blockHash,
      raw.blockNumber,
      raw.from: String,
      java.lang.Long.parseLong(raw.gas.drop(2),16),
      BigInt(Util.unhex(raw.gasPrice)),
      raw.maxFeePerGas.map(v => BigInt(Util.unhex(v))),
      raw.maxPriorityFeePerGas.map(v => BigInt(Util.unhex(v))),
      raw.hash,
      raw.input,
      Integer.parseInt(raw.nonce.drop(2),16),
      raw.to,
      i = raw.transactionIndex,
      v = BigInt(Util.unhex(raw.value)),
      typ = Integer.parseInt(raw.`type`.drop(2),16).toByte,
      // this.accessList,
      ch = raw.chainId.map(v => Integer.parseInt(v.drop(2),16)),
      s"${Integer.parseInt(raw.v.drop(2),16).toByte}:${raw.r}:${raw.s}"
  )

  def parseMempool(data:String,delta:Boolean=true):Seq[MempoolTx] = {
    if(data.isEmpty()) return Seq()
    try {
      if(data.stripLeading().startsWith("{")) {
        try {
          val ts = System.currentTimeMillis
          val result = data.parseJson.convertTo[RpcTxPoolResult]
          val txpool =  result.result
          
          val pending = txpool.pending.values.map(_.values).flatten
          val queued = txpool.queued.values.map(_.values).flatten
          log.debug(s"pending=${pending}")
          log.debug(s"queued=${queued}")
          
          val (pendingNew,pendingOld,pendingOut,queuedNew,queuedOld,queuedOut) = 
          if(delta) {
            val pendingSet:Set[RpcTxRaw] = pending.toSet
            val queuedSet:Set[RpcTxRaw] = queued.toSet
            
            val (pendingNew,pendingOld,pendingOut) = pendingDiff.diff(pendingSet)
            val (queuedNew,queuedOld,queuedOut) = queuedDiff.diff(queuedSet)
            
            (pendingNew.toSeq,pendingOld.toSeq,pendingOut.toSeq,
             queuedNew.toSeq,queuedOld.toSeq,queuedOut.toSeq)
          
          } else {
            (pending.toSeq,Seq(),Seq(),
            queued.toSeq,Seq(),Seq())
          }

          log.info(s"pending=[${pendingNew.size},${pendingOld.size},${pendingOut.size}],queued=[${queuedNew.size},${queuedOld.size},${queuedOut.size}]")
          
          pendingNew.map(raw => decodeMempoolTx(raw,ts,"pending")) ++ queuedNew.map(raw => decodeMempoolTx(raw,ts,"queued"))

        } catch {
          case e:Exception => 
            log.error(s"failed to parse: '${data}'",e)
            Seq()
        }
      } else {
        val m = data.split(",",-1).toList match {
          case rpc :: id :: result :: Nil => 
            log.error(s"not implemented: '${data}'")
            None
          case _ => {
            log.error(s"failed to parse: '${data}'")
            None
          }
        }
        m.toSeq
      }
    } catch {
      case e:Exception => 
        log.error(s"failed to parse: '${data}'",e)
        Seq()
    }
  }
}
