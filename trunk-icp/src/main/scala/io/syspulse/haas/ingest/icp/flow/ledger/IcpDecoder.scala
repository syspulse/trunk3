package io.syspulse.haas.ingest.icp.flow.ledger

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

import io.syspulse.haas.serde._

import io.syspulse.haas.ingest.icp.IcpURI

import io.syspulse.haas.ingest.icp.flow.ledger._
import io.syspulse.haas.ingest.icp.flow.ledger.IcpRpcJson

import io.syspulse.haas.core.RetryException
import io.syspulse.haas.ingest.Decoder

trait IcpDecoder[T] extends Decoder[T,IcpRpcBlock,IcpRpcBlock,Nothing,Nothing,IcpRpcBlock] {

  import IcpRpcJson._

  val log = Logger(s"${this}")
  
  // it really parses IcpTransactions into IcpBlock (Transaction)
  def parseBlock(data:String):Seq[IcpRpcBlock] = {
    if(data.isEmpty()) return Seq()
    
    // only JSON is supported
    if(data.stripLeading().startsWith("{")) {
      
      val transactions = try {
        data.parseJson.convertTo[IcpRpcTransactions]
      } catch {
        case e:Exception => 
          log.error(s"failed to parse: '${data}'",e)
          throw new RetryException(s"failed to parse: '${data}'")          
      }
      
      transactions.blocks.toSeq
      
    } else {
      log.error(s"failed to parse: '${data}'")
      throw new RetryException(s"failed to parse: not json: '${data}'")
    }    
  }
  
  def parseTransaction(data:String):Seq[IcpRpcBlock] = parseBlock(data) 

  def parseTx(data:String):Seq[Nothing] = {
    throw new Exception(s"Not supported: '${data}'")    
  }
  
  def parseTokenTransfer(data:String):Seq[Nothing] = {
    throw new Exception(s"Not supported: '${data}'")    
  }

  
  def parseEventLog(data:String):Seq[Nothing] = {
    throw new Exception(s"Not supported: '${data}'")    
  }

}
