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
import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson._

import io.syspulse.haas.ingest.Decoder
import io.syspulse.skel.util.DiffSet
import io.syspulse.haas.ingest.Config
import io.syspulse.skel.blockchain.eth.EthUtil

import io.syspulse.skel.service.JsonCommon

trait RPCDecoder[T] extends Decoder[T,RpcBlock,RpcTx,RpcTokenTransfer,RpcLog,RpcTx] with JsonCommon {

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
      EthUtil.toOption(raw.input),

      //java.lang.Long.parseLong(raw.nonce.drop(2),16),
      EthUtil.toBigInt(raw.nonce),
      
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

  def parseHead(data:String):Option[RpcSubscriptionHeadResult] = {
    if(data.isEmpty()) return None
    
    // only JSON is supported
    if(data.stripLeading().startsWith("{")) {      
      val head = try {
        data.parseJson.convertTo[RpcSubscriptionHead]
      } catch {
        case e:Exception => 
          log.error(s"failed to parse: '${data}'",e)
          //log.error(s"failed to parse: '${data}'")
          throw new RetryException(s"failed to parse: '${data}'")          
      }

      if(head.result.isDefined) {
        // response to subscription, ignore
        return None
      }

      Some(head.params.get.result)
      
    } else {
      log.error(s"failed to parse: '${data}'")
      throw new RetryException(s"failed to parse: '${data}'")        
      //None
    }    
  }

  def formatAddr(addr:Option[String],fmt:String):Option[String] = { 
    if(!addr.isDefined) return None

    Option(formatAddr(addr.get,fmt))
  }

  def formatAddr(addr:String,fmt:String):String = {    
    if(addr == null) 
      return addr

    if(fmt.size == 0) 
      return addr

    fmt.charAt(0) match {
      case 'l' => addr.toLowerCase
      case 'u' => addr.toUpperCase
      case _ => addr
    }
  }

  // ---- Receipts --------------------------------------------------------------------------------------------------------------------------
  
  // legacy and very inefficient for the whole Block
  // However it is supported by 'anvil'
  def decodeReceiptsBatch(block: RpcBlock)(implicit config:Config,uri:String): Map[String,RpcReceipt] = {
    block.result.map(r => 
      decodeTxReceipts(r.transactions.map(_.hash).toIndexedSeq)
    ).getOrElse(Map())
  }

  def decodeTxReceipts(transactions: Seq[String])(implicit config:Config,uri:String): Map[String,RpcReceipt] = {
    
    if(transactions.size == 0)
      return Map()
    
    val receiptBatch = if(config.receiptBatch == -1) transactions.size else config.receiptBatch
    val ranges = transactions.grouped(receiptBatch).toSeq

    val receiptMap = ranges.view.zipWithIndex.map{ case(range,i) => {      
      //log.debug(s"transactions: ${b.transactions.size}")
        
      if(range.size > 0) {
        if(i != 0) {
          Thread.sleep(config.receiptThrottle)
        }

        val json = 
          "[" + range.map( txHash => 
            s"""{"jsonrpc":"2.0","method":"eth_getTransactionReceipt","params":["${txHash}"],"id":"${txHash}"}"""
          ).mkString(",") +
          "]"
          .trim.replaceAll("\\s+","")
          
        try {
          val receiptsRsp = requests.post(uri, data = json,headers = Map("content-type" -> "application/json"))        
          val receipts:Seq[(String,RpcReceipt)] = receiptsRsp.statusCode match {
            case 200 =>
              
              val batchRsp = receiptsRsp.text()//receiptsRsp.data.toString
              
              try {
                if(batchRsp.contains("""error""") && batchRsp.contains("""code""")) {
                  throw new Exception(s"${batchRsp}")
                }
                              
                val batchReceipts = batchRsp.parseJson.convertTo[List[RpcReceiptResultBatch]]

                val rr:Seq[RpcReceipt] = batchReceipts.flatMap { r => 
                  
                  if(r.result.isDefined) {
                    Some(r.result.get)
                  } else {
                    log.warn(s"could not get receipt: (tx=${r.id}): ${r}")
                    None
                  }
                }
                
                rr.map( r => r.transactionHash -> r).toSeq              

              } catch {
                case e:Exception =>
                  log.error(s"could not parse receipts batch: ${receiptsRsp}",e)
                  Seq()
              }
            case _ => 
              log.warn(s"could not get receipts batch: ${receiptsRsp}")
              Seq()
          }
          receipts
        } catch {
          case e:Exception =>
            log.error("failed to get receipts",e)
            Map()
        }

      } else
        Map()  
    }}.flatten.toMap 
    
    receiptMap
  }

  // --- Receipts via one call
  def decodeReceiptsBlock(block: RpcBlock)(implicit config:Config,uri:String): Map[String,RpcReceipt] = {
    val b = block.result.get

    if(b.transactions.size == 0)
      return Map()
            
    val receiptMap = {      
      //log.debug(s"transactions: ${b.transactions.size}")        

      val id = b.number
      val json =  s"""{"jsonrpc":"2.0","method":"eth_getBlockReceipts","params":["${b.number}"],"id":"${id}"}"""
        .trim.replaceAll("\\s+","")
        
      try {
        val receiptsRsp = requests.post(uri, data = json,headers = Map("content-type" -> "application/json"))
        val receipts:Seq[(String,RpcReceipt)] = receiptsRsp.statusCode match {
          case 200 =>
            
            val rsp = receiptsRsp.text()
            
            try {
              if(rsp.contains("""error""") && rsp.contains("""code""")) {
                throw new Exception(s"${b.number}: ${rsp}")
              }
            
              val rr = rsp.parseJson.convertTo[RpcBlockReceiptsResult].result
              rr.map( r => r.transactionHash -> r).toSeq

            } catch {
              case e:Exception =>
                log.error(s"failed to parse receipts: ${b.number}: ${receiptsRsp}: rsp=${rsp}",e)
                Seq()
            }
          case _ => 
            log.warn(s"failed to get receipts: ${b.number}: ${receiptsRsp}")
            Seq()
        }
        receipts
      } catch {
        case e:Exception =>
          log.error(s"failed to get block receipts: ${b.number}",e)
          Map()
      }

    }.toMap 

    // if(receiptMap.size == b.transactions.size) {
    //   // commit cursor only if all transactions receipts recevied !
    //   cursor.commit(toLong(b.number))
    // }

    receiptMap
  }

  def decodeReceipts(block: RpcBlock)(implicit config:Config,uri:String): Map[String,RpcReceipt] = {    
    if(config.receiptDelay > 0L) {
      // ATTENTION: Sleep inside Akka stream Thread !
      Thread.sleep(config.receiptDelay)
    }

    config.receiptRequest match {
      case "block" => decodeReceiptsBlock(block)
      case "batch" => decodeReceiptsBatch(block)
      case _ => decodeReceiptsBlock(block)
    }
  }

  def decodeTransactions(block: RpcBlock)(implicit config:Config,uri:String): (Seq[Transaction],Seq[RpcReceipt]) = {
    val b = block.result.get

    val ts = EthUtil.toLong(b.timestamp)
    val block_number = EthUtil.toLong(b.number)

    log.info(s"Block(${block_number},${b.transactions.size})")
      
    val receipts:Map[String,RpcReceipt] = decodeReceipts(block)
    
    val txx = b.transactions
    .filter(tx => {
      config.filter.size == 0 || config.filter.contains(tx.hash)
    })
    .map{ tx:RpcTx => {
      val transaction_index = EthUtil.toLong(tx.transactionIndex).toInt
      val logs = receipts.get(tx.hash).get.logs
      val receipt = receipts.get(tx.hash)
      
      Transaction(
        ts * 1000L,
        transaction_index,
        tx.hash,
        block_number,

        formatAddr(tx.from,config.formatAddr),
        formatAddr(tx.to,config.formatAddr),
        
        EthUtil.toLong(tx.gas),
        EthUtil.toBigInt(tx.gasPrice),
        tx.input,
        EthUtil.toBigInt(tx.value),

        EthUtil.toBigInt(tx.nonce),
        
        tx.maxFeePerGas.map(EthUtil.toBigInt(_)), //tx.max_fee_per_gas,
        tx.maxPriorityFeePerGas.map(EthUtil.toBigInt(_)), //tx.max_priority_fee_per_gas, 

        tx.`type`.map(r => EthUtil.toLong(r).toInt),

        receipt.map(r => EthUtil.toLong(r.cumulativeGasUsed)).getOrElse(0L), //0L,//tx.receipt_cumulative_gas_used, 
        receipt.map(r => EthUtil.toLong(r.gasUsed)).getOrElse(0L), //0L,//tx.receipt_gas_used, 
        receipt.map(r => formatAddr(r.contractAddress,config.formatAddr)).flatten, //tx.receipt_contract_address, 
        Some(b.receiptsRoot), //tx.receipt_root, 
        receipt.flatMap(r => r.status.map(EthUtil.toLong(_).toInt)), //tx.receipt_status, 
        receipt.map(_.effectiveGasPrice.map(r => EthUtil.toBigInt(r))).flatten, //tx.receipt_effective_gas_price

        logs = Some(logs.map( r => {
          EventTx(
            EthUtil.toLong(r.logIndex).toInt,
            formatAddr(r.address,config.formatAddr),
            r.data,
            r.topics
          )
        }))
      )
    }}.toSeq

    // if(receipts.size == b.transactions.size) {
    //   // commit cursor only if all transactions receipts recevied !
    //   cursor.commit(block_number)
    // }

    (txx,receipts.values.toSeq)
  }
}
