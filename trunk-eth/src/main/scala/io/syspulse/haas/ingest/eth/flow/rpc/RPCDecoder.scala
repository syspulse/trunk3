package io.syspulse.haas.ingest.eth.flow.rpc

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

import io.syspulse.haas.ingest.eth._

import io.syspulse.haas.ingest.eth.EthURI

import io.syspulse.haas.ingest.Config
import io.syspulse.haas.ingest.eth.rpc._
import io.syspulse.haas.ingest.eth.rpc.EthRpcJson

import io.syspulse.haas.ingest.Decoder
import io.syspulse.haas.ingest.eth.BlockJson
import io.syspulse.haas.ingest.eth.EventJson
import io.syspulse.haas.ingest.eth.TokenTransferJson
import io.syspulse.haas.ingest.eth.TxJson

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
              
      block.result.get.transactions

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

}
