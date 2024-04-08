package io.syspulse.haas.ingest.eth.flow.rpc3

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

import io.syspulse.haas.ingest.Config
import io.syspulse.haas.ingest.eth._

import io.syspulse.haas.ingest.eth._
import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson._
import io.syspulse.haas.ingest.eth.MempoolJson._
import io.syspulse.haas.ingest.PipelineIngest
import io.syspulse.haas.ingest.eth.flow.rpc3.RpcTxPoolResult

abstract class PipelineRpcMempoolWS[E <: skel.Ingestable](config:Config)
                                                       (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineMempoolWS[MempoolTransaction,MempoolTransaction,E](config) {
  
  def apiSuffix():String = s"/mempool.stream"

  // only json is supported !
  override def parse(data:String):Seq[MempoolTransaction] = {
    val pool = parseMempoolWS(data,true)
    pool.map(mtx => {
      MempoolTransaction(
        ts = System.currentTimeMillis(),
        hash = mtx.params.result
      )
    })
  }

  def convert(mtx: MempoolTransaction): MempoolTransaction = {    
    val json = s"""{"jsonrpc":"2.0","method":"debug_traceTransaction","params":["${mtx.hash}",{"tracer":"callTracer"}],"id": ${mtx.ts}}"""

    val rsp = requests.post(config.rpcUrl, data = json,headers = Map("content-type" -> "application/json"))
    val body = rsp.text()
    log.info(s"body=${body}")
    mtx
  }
}

class PipelineMempoolStream(config:Config) extends PipelineRpcMempoolWS[MempoolTransaction](config) {

  def transform(tx: MempoolTransaction): Seq[MempoolTransaction] = Seq(tx)    
}
