package io.syspulse.haas.ingest.eth.flow.raw

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

import akka.util.ByteString
import akka.http.javadsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Flow

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

import java.util.concurrent.TimeUnit

import io.syspulse.haas.ingest.eth.TransactionJson
import io.syspulse.haas.ingest.eth.TransactionJson._
import io.syspulse.haas.ingest.eth._
import io.syspulse.haas.ingest.Config
import io.syspulse.haas.ingest.eth.EthEtlJson._
import io.syspulse.haas.ingest.PipelineIngest

case class Textline(txt:String) extends skel.Ingestable {
  override def getKey: Option[Any] = Some(txt.hashCode())
  override def toString = txt
}

trait TextlineJsonProtocol extends DefaultJsonProtocol {
  private val log = Logger(s"${this}")

  implicit object TextlineJsonFormat extends RootJsonFormat[Textline] {
    def write(t: Textline) = {
      if(t.txt.isBlank()) {
        JsObject()
      }
      else {      
        try {
          t.txt.parseJson
        } catch {
          case e:Exception =>
            log.warn(s"failed to convert to json: ${e.getMessage()}")
            JsString(t.txt)            
        }
      }
    }

    def read(value: JsValue) = value match {
      case JsString(str) => Textline(str)
      case _ => deserializationError("plain text expected")
    }
  }

  implicit val fmt = jsonFormat1(Textline.apply _)
}

object TextlineJson extends TextlineJsonProtocol { 
}

import TextlineJson._

class PipelineRaw(config:Config) extends PipelineIngest[Textline,Textline,Textline](config) {
  
  def apiSuffix():String = s"/raw"

  def convert(data:Textline):Textline = data
  def parse(data:String):Seq[Textline] = Seq(Textline(data))
  override def process:Flow[Textline,Textline,_] = Flow[Textline].map(data => data)
  def transform(data: Textline): Seq[Textline] = Seq(data)
    
}
