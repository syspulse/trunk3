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

import io.syspulse.haas.core.RetryException
import io.syspulse.haas.ingest.Decoder
import java.time.Instant

trait StellarDecoder[T] extends Decoder[T,StellarRpcBlock,StellarRpcTransaction,Nothing,Nothing,StellarRpcTransaction] {

  val log = Logger(s"${this}")

  import StellarRpcJson._  
  
  // "2024-02-04T14:25:24Z"
  //val tsFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
  def parseTs(ts:String) = {
    try {
      Instant.from(DateTimeFormatter.ISO_INSTANT.parse(ts)).toEpochMilli()
    } catch {
      case e:Exception =>
        log.error(s"failed to parse timestamp: ${ts}",e)
        throw e
    }
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
