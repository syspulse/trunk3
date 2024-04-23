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
import io.syspulse.haas.ingest.eth.CallTraceJson._
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

  def convert(tx: MempoolTransaction): MempoolTransaction = tx
    

}

class PipelineMempoolStream(config:Config) extends PipelineRpcMempoolWS[CallTrace](config) {

  def transform(mtx: MempoolTransaction): Seq[CallTrace] = {
    // val json = s"""{"jsonrpc":"2.0","method":"debug_traceTransaction","params":["${mtx.hash}",{"tracer":"callTracer"}],"id": ${mtx.ts}}"""
    // val rsp = requests.post(config.rpcUrl, data = json,headers = Map("content-type" -> "application/json"))
    // val body = rsp.text()
    // log.info(s"body=${body}")
    
    val tx = {
      val json = s"""{"jsonrpc":"2.0","method":"eth_getTransactionByHash","params":["${mtx.hash}"],"id": ${mtx.ts}}"""
      val rsp = requests.post(config.rpcUrl, data = json,headers = Map("content-type" -> "application/json"))
      val body = rsp.text()    
      //log.debug(s"body=${body}")
      val r = body.parseJson.convertTo[RpcMempoolTransactionResult]
      if(! r.result.isDefined)
        return Seq()

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
        "id": 0}
        """.trim.replaceAll("\\s+","")

      //log.debug(s"${json}")

      val rsp = requests.post(config.rpcUrl, data = json,headers = Map("content-type" -> "application/json"))
      val body = rsp.text()
      log.debug(s"body=${body}")

      if(body.contains(""""code":-32000""")) {
        log.warn(s"${body}")
      }

      body
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
        "id": 0}
        """.trim.replaceAll("\\s+","")

      //log.debug(s"${json}")

      val rsp = requests.post(config.rpcUrl, data = json,headers = Map("content-type" -> "application/json"))
      val body = rsp.text()
      log.debug(s"body=${body}")

      if(body.contains(""""code":-32000""")) {
        log.warn(s"${body}")
      }

      body
    }
    
    Seq(
      CallTrace(ts = System.currentTimeMillis(),hash = tx.hash, state = state, calls = calls)
    )
  
  }
}
