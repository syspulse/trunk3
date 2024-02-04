package io.syspulse.haas.ingest.stellar.flow.horizon

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
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime

import io.syspulse.haas.serde._

import io.syspulse.haas.ingest.stellar.StellarURI
import io.syspulse.haas.ingest.stellar.flow.horizon._
import io.syspulse.haas.ingest.stellar.flow.horizon.StellarRpcJson

import io.syspulse.haas.ingest.Decoder

trait StellarDecoder[T] extends Decoder[T,StellarRpcBlock,StellarRpcTransaction,Nothing,Nothing,StellarRpcTransaction] {

  protected val log = Logger(s"${this}")

  import StellarRpcJson._  
  
  def toLong(data:String) = java.lang.Long.parseLong(data.stripPrefix("0x"),16)
  def toBigInt(data:String) = BigInt(Util.unhex(data))
  def toOption(data:String) = if(data.isEmpty() || data=="0x") None else Some(data)
  def toOptionLong(data:String) = if(data.isEmpty() || data=="0x") None else Some(toLong(data))

  // "2024-02-04T14:25:24Z"
  val tsFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmssZ")
  def parseTs(ts:String) = {
    ZonedDateTime.parse(ts,tsFormat).toInstant().toEpochMilli()
  }

  // it really parses StellarTransactions into StellarBlock (Transaction)
  def parseBlock(data:String):Seq[StellarRpcBlock] = {
    if(data.isEmpty()) return Seq()
    
    // only JSON is supported
    if(data.stripLeading().startsWith("{")) {
      
      val b = try {
        data.parseJson.convertTo[StellarRpcBlock]
      } catch {
        case e:Exception => 
          log.error(s"failed to parse: '${data}'",e)
          throw new RetryException(s"failed to parse: '${data}'")          
      }

      Seq(b)
      
    } else {
      log.error(s"failed to parse: '${data}'")
      throw new RetryException(s"failed to parse: not json: '${data}'")
    }    
  }

  def parseTransaction(data:String):Seq[StellarRpcTransaction] = {
    if(data.isEmpty()) return Seq()
    
    // only JSON is supported
    if(data.stripLeading().startsWith("{")) {
      
      val t = try {
        data.parseJson.convertTo[StellarRpcTransaction]
      } catch {
        case e:Exception => 
          log.error(s"failed to parse: '${data}'",e)
          throw new RetryException(s"failed to parse: '${data}'")          
      }

      Seq(t)
      
    } else {
      log.error(s"failed to parse: '${data}'")
      throw new RetryException(s"failed to parse: not json: '${data}'")
    }    
  }
    
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
