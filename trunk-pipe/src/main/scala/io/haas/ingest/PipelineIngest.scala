package io.haas.ingest

import java.util.concurrent.atomic.AtomicLong
import io.syspulse.skel.ingest.flow.Flows

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import java.util.concurrent.TimeUnit
import com.typesafe.scalalogging.Logger

import akka.util.ByteString
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Keep
import akka.stream.RestartSettings

import akka.http.scaladsl.model.{HttpRequest,HttpMethods,HttpEntity,ContentTypes}
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util
import io.syspulse.skel.config._

import io.syspulse.skel.ingest._
import io.syspulse.skel.ingest.store._

import io.syspulse.skel.ingest.flow.Pipeline

import spray.json._
import DefaultJsonProtocol._

import com.github.mjakubowski84.parquet4s.{ParquetRecordEncoder,ParquetSchemaResolver}
import io.syspulse.skel.serde.Parq._

import io.haas.ingest.Config
import io.jvm.uuid._

import io.haas.intercept.Script
import io.haas.intercept.ScriptInterceptor
import io.haas.intercept.InterceptResult
import io.syspulse.skel.blockchain.Blockchain

case class Alert(
  did:String,
  eid:String,
  sid:String,
  category:String,
  `type`:String,
  severity:Double,
  ts:Long,
  blockchain:String,
  metadata:Map[String,String] = Map()
) extends skel.Ingestable {
  override def getKey:Option[Any] = Some(eid)
}

case class Alerts(alerts:Seq[Alert]) extends skel.Ingestable {
  override def getKey:Option[Any] = None
}

object AlertJson extends DefaultJsonProtocol {  
  implicit val jf_alert = jsonFormat9(Alert)
  implicit val jf_alerts = jsonFormat1(Alerts)
}

abstract class PipelineIngest[T,O <: skel.Ingestable,E <: skel.Ingestable]
  (config:Config)
  (implicit val fmt:JsonFormat[E],
    parqEncoders0:ParquetRecordEncoder[E],parsResolver0:ParquetSchemaResolver[E]
  )
  extends PipelineIngestable[T,O,E,Alerts](config)(fmt, AlertJson.jf_alerts, parqEncoders0, parsResolver0, implicitly[ParquetRecordEncoder[Alerts]], implicitly[ParquetSchemaResolver[Alerts]]) {
    
  private val log = Logger(getClass)
 
  override val retrySettings:Option[RestartSettings] = Some(RestartSettings(
    minBackoff = FiniteDuration(1000,TimeUnit.MILLISECONDS),
    maxBackoff = FiniteDuration(1000,TimeUnit.MILLISECONDS),
    randomFactor = 0.2
  ))

  val blockchain = Blockchain(config.interceptorBlockchain)
  
  // interceptor can be changed in runtime
  @volatile
  var interceptor = if(config.script.isEmpty()) 
    None 
  else 
    Some(new ScriptInterceptor(Seq(Script("0",src = config.script,ts0 = System.currentTimeMillis))))

  // Dirty workaround to quick configure additiona callback for Default Interception Classes
  // configurable callback  
  @volatile
  var interceptCallaback: Option[(InterceptResult) => Unit] = None
  def setInterceptCallack(callback:(InterceptResult) => Unit) = {
    interceptCallaback = Some(callback)
  }

  def setInterceptor(id:String,script:String) = {
    interceptor = Some(new ScriptInterceptor(
      Seq(Script(id,src = script,ts0 = System.currentTimeMillis))
    ))
  }

  // default is Ext interceptor
  def interception(e:E) = interceptionExt(e)

  def interceptionExt(e:E):Seq[Alerts] = {
    interceptor match {
      case Some(interceptor) => 
        
        // run intercept    
        val r = interceptor.scan[E](e)
        
        if(interceptCallaback.isDefined) {
          r.foreach{ r => 
            // call callback
            (interceptCallaback.get)(r) 
        }}

        r.map(r => { 
          
          val alert = Alert(
            did = config.interceptorName,
            eid = UUID.random.toString,
            sid = config.interceptorSid,
            category = config.interceptorCat,
            `type` = config.interceptorType,
            severity = config.interceptorSeverity,
            ts = System.currentTimeMillis(),
            blockchain = blockchain.name,
            metadata = Map(
              "tx_hash" -> r.txHash,
              "monitored_contract" -> config.interceptorContract,
            ) ++ r.data
          )

          Alerts(alerts = Seq(alert))
        })
      
      case None => 
        Seq()
    }
  }

  // Additional sink where data is piped
  // override def sink0() = {    
  //   import io.hacken.ext.core.ExtractorJson._
  //   import io.hacken.ext.core.Blockchain
        
  //   val f = Flow[E].mapConcat( e => {
  //     interception[io.hacken.ext.core.Events](e)
  //   })

  //   val s0 = sinking[io.hacken.ext.core.Events](config.alertOutput)
  //   f.to(s0)    
  // }

  def setCursorBlock(cursor:CursorBlock,decodeBlocks:Seq[String] => Seq[Long]) = {
    (config.block.split("://").toList match {
      // start from latest and save to file
      case "latest" :: file :: Nil => 
        cursor.setFile(file).read()              
        "latest"
      case "last" :: file :: Nil => 
        cursor.setFile(file).read()
        "latest"
      case "latest" :: Nil =>  // use default file
        cursor.setFile("").read()              
        "latest"

      case "file" :: file :: Nil => cursor.setFile(file).read()
      case "file" :: Nil => cursor.read()

      case "list" :: file :: Nil => 
        val data = os.read(os.Path(file,os.pwd))
        val list = data.split("[\\n,]").filter(!_.isBlank).map(_.trim.toLong)
        cursor.setList(list.toSeq)
        list.head.toString

      case "rpc" :: Nil =>         
        // use this option with filter with transactions and find out all blocks for those transactions
        val bb0 = decodeBlocks(config.filter)
        if(bb0.size == 0) {
          log.error(s"blocks not found: ${config.filter}")
          sys.exit(3)
        }
        val bb = bb0.sorted.distinct
        cursor.setList(bb)
        bb.head.toString

      // start block and save to file (10://file.txt)
      case block :: file :: Nil => cursor.setFile(file).read(); 
        block

      case _ => 
        // supports a list of blocks
        val bb = config.block.split(",").map(_.trim.toLong).sorted.toSeq
        
        if(bb.size > 1) 
          cursor.setList(bb)

        bb.head.toString
    })
  }

  def decodeSingle(rsp:String):Seq[String] = Seq(rsp)

  def decodeBatch(rsp:String):Seq[String] = {
    // ATTENTION !!!
    // very inefficient, optimize with web3-proxy approach 
    // val jsonBatch = ujson.read(rsp)
    // jsonBatch.arr.map(a => a.toString()).toSeq

    // Batch JSON-RPC responses are arrays; unwrap to per-item JSON strings.
    // Fast path avoids re-stringifying whole response and is allocation-light.    
    rsp.parseJson match {
      case JsArray(elements) =>
        elements.iterator.map(_.compactPrint).toVector        
      case _ =>
        Seq(rsp)
    }
  }
}
