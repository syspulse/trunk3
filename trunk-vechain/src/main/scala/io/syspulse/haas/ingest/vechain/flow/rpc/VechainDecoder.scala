package io.syspulse.haas.ingest.vechain.flow.rpc

import java.util.concurrent.atomic.AtomicLong
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

import com.github.tototoshi.csv._

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util
import io.syspulse.skel.config._

import io.syspulse.skel.ingest._

import spray.json._
import DefaultJsonProtocol._
import java.util.concurrent.TimeUnit

import io.syspulse.haas.ingest.vechain.flow.rpc._
import io.syspulse.haas.ingest.vechain.flow.rpc.VechainRpcJson

import io.syspulse.haas.ingest.Config
import io.syspulse.haas.core.RetryException
import io.syspulse.haas.ingest.Decoder

trait VechainDecoder[T] extends Decoder[T,RpcBlock,RpcTx,Nothing,Nothing,RpcTx] {

  protected val log = Logger(s"${this}")

  import VechainRpcJson._ 
    
  def parseBlock(data:String):Seq[RpcBlock] = {
    if(data.isEmpty()) return Seq()
    
    // only JSON is supported
    if(data.stripLeading().startsWith("{")) {
      
      val block = try {
        data.parseJson.convertTo[RpcBlock]
      } catch {
        case e:Exception => 
          log.error(s"failed to parse: '${data}'",e)
          throw new RetryException(s"failed to parse: '${data}'")          
      }
      
      Seq(block)
      
    } else {
      log.error(s"failed to parse: '${data}'")
      throw new RetryException(s"failed to parse: '${data}'")
    }    
  }

  def parseTx(data:String):Seq[RpcTx] = parseTransaction(data) 

  def parseTransaction(data:String):Seq[RpcTx] = {
    if(data.isEmpty()) return Seq()
    
    // only JSON is supported
    if(data.stripLeading().startsWith("{")) {
      
      val tx = try {
        data.parseJson.convertTo[RpcTx]
      } catch {
        case e:Exception => 
          log.error(s"failed to parse: '${data}'",e)
          throw new RetryException(s"failed to parse: '${data}'")          
      }
      
      Seq(tx)
      
    } else {
      log.error(s"failed to parse: '${data}'")
      throw new RetryException(s"failed to parse: '${data}'")
    }    
  }
  
  def parseTokenTransfer(data:String):Seq[Nothing] = {
    throw new Exception(s"Not supported: '${data}'")    
  }

  
  def parseEventLog(data:String):Seq[Nothing] = {
    throw new Exception(s"Not supported: '${data}'")    
  }

  def parseSingleTx(txHash:String)(config:Config,uri:String):RpcTx = {
    val rsp = requests.get(s"${uri}/transactions/${txHash}",
        headers = Map("content-type" -> "application/json")
      )
    //log.info(s"rsp=${rsp.statusCode}: ${rsp.text()}")
    rsp.statusCode match {
      case 200 => //
      case _ => 
        // retry
        log.error(s"RPC error: ${rsp.statusCode}: ${rsp.text()}")
        throw new RetryException("")
    }
    
    val tx = rsp.text().parseJson.convertTo[RpcTx] 
    tx
  }

  def parseBatchTx(blockNumber:Long)(config:Config,uri:String):Seq[RpcTxBlock] = {
    val rsp = requests.get(s"${uri}/blocks/${blockNumber}?expanded=true",
        headers = Map("content-type" -> "application/json")
      )
    //log.info(s"rsp=${rsp.statusCode}: ${rsp.text()}")
    rsp.statusCode match {
      case 200 => //
      case _ => 
        // retry
        log.error(s"RPC error: ${rsp.statusCode}: ${rsp.text()}")
        throw new RetryException("")
    }
    
    val txx = rsp.text().parseJson.convertTo[RpcBlockTx]
    txx.transactions
  }

}
