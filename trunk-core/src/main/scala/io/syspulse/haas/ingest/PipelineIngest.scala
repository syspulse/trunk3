package io.syspulse.haas.ingest

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
import io.syspulse.skel.serde.Parq._
import com.github.mjakubowski84.parquet4s.{ParquetRecordEncoder,ParquetSchemaResolver}

import io.syspulse.haas.ingest.Config
import io.jvm.uuid._

case class Output(data:String) extends skel.Ingestable

object OutputJson extends DefaultJsonProtocol {  
  implicit val jf_output = jsonFormat1(Output)  
}
import OutputJson._

// class PipelineIntercept[O <: skel.Ingestable](config:Config)
//                                                                        (implicit val fmt:JsonFormat[Output],parqEncoders:ParquetRecordEncoder[Output],parsResolver:ParquetSchemaResolver[Output])
//   extends Pipeline[O,O,Output](config.feed,config.output,config.throttle,config.delimiter,config.buffer,format=config.format) {
  
//   def process:Flow[O,O,_] = Flow[O].map(o => {
//     o
//   })

//   def transform(o: O) = Seq(Output(s"data=${o.toString.size}"))
// }


abstract class PipelineIngest[T,O <: skel.Ingestable,E <: skel.Ingestable](config:Config)
                                                                       (implicit val fmt:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E])
  extends Pipeline[T,O,E](config.feed,config.output,config.throttle,config.delimiter,config.buffer,format=config.format) {
  
  private val log = Logger(s"${this}")
  
  var latestTs:AtomicLong = new AtomicLong(0)
    

  override def getRotator():Flows.Rotator = 
    new Flows.RotatorTimestamp(() => {
      latestTs.get()
    })

  override def getFileLimit():Long = config.limit
  override def getFileSize():Long = config.size

  def filter():Seq[String] = config.filter
  def apiSuffix():String

  def convert(t:T):O

  //def transform(o: O) = Seq(o)

  def process:Flow[T,O,_] = Flow[T].map(t => {
    val o = convert(t)
    o
  })

  override def sink0() = {
    import io.syspulse.ext.core.ExtractorJson._
    import io.syspulse.ext.core.Blockchain
        
    val f = Flow[E].map( e => {
      
      val event = io.syspulse.ext.core.Event(
        did = "Interceptor",
        eid = UUID.random.toString,
        sid = "trunk:interceptor",
        category = "EVENT",
        `type` = "monitor",
        severity = 0.15,
        ts = System.currentTimeMillis(),
        blockchain = io.syspulse.ext.core.Blockchain("ethereum"),
        metadata = Map(
          "monitored_contract" -> "0x0000000000000000000000000000000000000007", // use artificical contract name
          "output" -> ""
        )
      )
        
      event
    })

    val s0 = sinking[io.syspulse.ext.core.Event](config.outputAlert)
    f.to(s0)    
  }

}
