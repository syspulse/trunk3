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
import io.syspulse.haas.ingest.Config
import io.syspulse.haas.ingest.IngestUtil

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

  def decodeMempoolTx(raw:RpcTxRaw,ts:Long,pool:String):MempoolTx = 
    MempoolTx(
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
      IngestUtil.toOption(raw.input),

      //java.lang.Long.parseLong(raw.nonce.drop(2),16),
      IngestUtil.toBigInt(raw.nonce),
      
      raw.to,
      i = raw.transactionIndex,
      v = BigInt(Util.unhex(raw.value)),
      typ = Integer.parseInt(raw.`type`.drop(2),16).toByte,
      // this.accessList,
      chid = raw.chainId.map(v => Integer.parseInt(v.drop(2),16)),
      sig = raw.v.map(_ => s"${Integer.parseInt(raw.v.get.drop(2),16).toByte}:${raw.r.get}:${raw.s.get}")
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

  def parseMempoolWS(data:String,delta:Boolean=true):Seq[RpcWsMempoolResult] = {
    if(data.isEmpty()) return Seq()
    try {
      if(data.stripLeading().startsWith("{")) {
        try {
          val result = data.parseJson.convertTo[RpcWsMempoolResult]
          Seq(result)          

        } catch {
          case e:Exception => 
            // check if it is a result
            try {
              data.parseJson.convertTo[RpcResult]
              // ignore subscription result
              Seq()
            } catch {
              case e:Exception => 
                log.error(s"failed to parse: '${data}'",e)
                Seq()
            }            
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

  def traceMempoolTx(txHash: String)(implicit config:Config): Vector[CallTrace] = {    
  //   val block = tx.b match {
  //     case Some(n) => s"0x${n.toHexString}"
  //     case None => "latest"
  //   }

  //   val json = s"""{"jsonrpc":"2.0","method":"debug_traceCall",
  //     "params":[
  //       {
  //         "from":"${tx.from}",
  //         "to":"${tx.to.getOrElse("")}",
  //         "gas":"0x${tx.gas.toHexString}",
  //         "gasPrice":"0x${tx.fee.getOrElse(BigInt(0)).bigInteger.toString(16)}",
  //         "value":"${tx.v}",
  //         "data":"${tx.inp}"
  //       }, 
  //       "${block.toString}",
  //       {
  //         "tracer":"prestateTracer",
  //         "tracerConfig":{
  //           "diffMode":true
  //         }
  //       }
  //     ],
  //     "id": ${tx.ts}}
  //     """.trim.replaceAll("\\s+","")

  //   val rsp = requests.post(config.rpcUrl, data = json,headers = Map("content-type" -> "application/json"))
  //   val body = rsp.text()
  //   log.info(s"body=${body}")
  //   body
  // }

    var err = false

    val tx = {
      val json = s"""{"jsonrpc":"2.0","method":"eth_getTransactionByHash","params":["${txHash}"],"id": ${System.currentTimeMillis}}"""
      val rsp = requests.post(config.rpcUrl, data = json,headers = Map("content-type" -> "application/json"))
      val body = rsp.text()    
      //log.debug(s"body=${body}")
      val r = try {
        body.parseJson.convertTo[RpcMempoolTransactionResult]
      } catch {
        case e:Exception => 
          err = true
          log.error(s"failed to parse: '${body}'",e)
          return Vector.empty
      }
      if(! r.result.isDefined)
        return Vector.empty

      r.result.get
    }

    // collect States
    val state = {
      val json = s"""{"jsonrpc":"2.0","method":"debug_traceCall",
        "params":[
          {
            "from":"${tx.from}",
            "to":${if(tx.to.isDefined) "\""+tx.to.get+"\"" else "null"},
            "gas":"${tx.gas}",
            "gasPrice":"${tx.gasPrice}",
            "value":${if(tx.value.isDefined) "\""+tx.value.get+"\"" else "null"},
            "data":${if(tx.input.isDefined) "\""+tx.input.get+"\"" else "null"}
          },
          "latest",
          {
            "tracer":"prestateTracer",
            "tracerConfig":{
              "diffMode":true
            }
          }
        ],
        "id": ${System.currentTimeMillis}}
        """.trim.replaceAll("\\s+","")
      
      val rsp = requests.post(config.rpcUrl, data = json,headers = Map("content-type" -> "application/json"))
      val body = rsp.text()
      log.debug(s"body=${body}")

      if(body.contains(""""code":-32000""")) {
        err = true
        log.debug(s"state: ${body}")
        return Vector.empty
      }

      val r = try {
        body.parseJson.convertTo[RpcTraceStateResult]
      } catch {
        case e:Exception => 
          err = true
          log.error(s"failed to parse: '${body}'",e)
          return Vector.empty
      }

      if(! r.result.isDefined)
        return Vector.empty

      r.result.get.toString
    }

    // collect States
    val calls = {
      val json = s"""{"jsonrpc":"2.0","method":"debug_traceCall",
        "params":[
          {
            "from":"${tx.from}",
            "to":${if(tx.to.isDefined) "\""+tx.to.get+"\"" else "null"},
            "gas":"${tx.gas}",
            "gasPrice":"${tx.gasPrice}",
            "value":${if(tx.value.isDefined) "\""+tx.value.get+"\"" else "null"},
            "data":${if(tx.input.isDefined) "\""+tx.input.get+"\"" else "null"}
          },
          "latest",
          {
            "tracer":"callTracer",
            "tracerConfig":{
              "withLogs":true
            }
          }
        ],
        "id": ${System.currentTimeMillis}}
        """.trim.replaceAll("\\s+","")

      //log.debug(s"${json}")

      val rsp = requests.post(config.rpcUrl, data = json,headers = Map("content-type" -> "application/json"))
      val body = rsp.text()
      log.debug(s"body=${body}")

      if(body.contains(""""code":-32000""")) {
        err = true
        log.debug(s"calls: ${body}")
        return Vector.empty
      }

      val r = try {
        body.parseJson.convertTo[RpcTraceCallResult]
      } catch {
        case e:Exception => 
          err = true
          log.error(s"failed to parse: '${body}'",e)
          return Vector.empty
      }

      if(! r.result.isDefined)
        return Vector.empty

      r.result.get.toString
    }

    if(err) {
      return Vector.empty
    }
    
    Vector(
      CallTrace(ts = System.currentTimeMillis(),hash = tx.hash, state = state, calls = calls)
    )
  }

  // ----Reorg engines --------------------------------------------------------------------------------------------------------
  // def isReorg1(lastBlock:String)(implicit reorg:ReorgBlock) = {
  //   val r = ujson.read(lastBlock)
  //   val result = r.obj("result").obj
    
  //   val blockNum = java.lang.Long.decode(result("number").str).toLong
  //   val blockHash = result("hash").str
  //   val ts = java.lang.Long.decode(result("timestamp").str).toLong
  //   val txCount = result("transactions").arr.size

  //   // check if reorg
  //   val rr = reorg.isReorg(blockNum,blockHash)
    
  //   if(rr.size > 0) {
      
  //     log.warn(s"Reorg1:reorg block: >>>>>>>>> ${blockNum}/${blockHash}: reorgs=${rr}")
  //     os.write.append(os.Path("REORG",os.pwd),s"${ts},${blockNum},${blockHash},${txCount}}")
  //     reorg.reorg(rr)
  //     (true,true)
      
  //   } else {
      
  //     val fresh = reorg.cache(blockNum,blockHash,ts,txCount)
  //     (fresh,false)
  //   }
  // }

  // // new flow without duplicates
  // def isReorg2(lastBlock:String)(implicit reorg:ReorgBlock) = {    

  //   val r = ujson.read(lastBlock)
  //   val result = r.obj("result").obj
    
  //   val blockNum = java.lang.Long.decode(result("number").str).toLong
  //   val blockHash = result("hash").str
  //   val ts = java.lang.Long.decode(result("timestamp").str).toLong
  //   val txCount = result("transactions").arr.size
    
  //   // check if reorg
  //   val rr = reorg.isReorg(blockNum,blockHash)
    
  //   if(rr.size > 0) {
      
  //     log.warn(s"Reorg2: reorg block: >>>>>>>>> ${blockNum}/${blockHash}: reorgs=${rr}")
  //     os.write.append(os.Path("REORG",os.pwd),s"${ts},${blockNum},${blockHash},${txCount}}")
  //     reorg.reorg(rr)
  //     (true,true)
      
  //   } else {
      
  //     reorg.cache(blockNum,blockHash,ts,txCount)
  //     (true,false)
  //   }
  // }
  
}
