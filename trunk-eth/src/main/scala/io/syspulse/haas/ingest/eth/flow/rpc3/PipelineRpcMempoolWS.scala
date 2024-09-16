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

import io.syspulse.haas.ingest.IngestUtil

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
    pool.map(mm => {
      MempoolTransaction(
        ts = System.currentTimeMillis(),
        hash = mm.params.result
      )
    })
  }

  def convert(m: MempoolTransaction): MempoolTransaction = m

  def getMempoolTx(m: MempoolTransaction,trace:Option[Array[CallTrace]]): MempoolTx = {
    
    val tx:Option[RpcMempoolTransaction] = {
      val json = s"""{"jsonrpc":"2.0","method":"eth_getTransactionByHash","params":["${m.hash}"],"id": ${System.currentTimeMillis}}"""
      val rsp = requests.post(config.rpcUrl, data = json,headers = Map("content-type" -> "application/json"))
      val body = rsp.text()    
            
      val tx = try {
        val r = body.parseJson.convertTo[RpcMempoolTransactionResult]
        if(! r.result.isDefined)
          None
        else
          r.result
      } catch {
        case e:Exception => 
          log.error(s"failed to parse: '${body}'",e)
          None
      }

      tx
    }

    log.debug(s"tx=${tx}")

    val mtx = tx.map(rmx => 
      MempoolTx(
        ts = m.ts,
        pool = "0",     // NOTE: CHANGE TO Byte: pending - 0, queued - 1,
        bhash = rmx.blockHash,
        b = IngestUtil.toLong(rmx.blockNumber),
        from = rmx.from,
        gas = IngestUtil.toLong(rmx.gas),
        p = BigInt(0),
        fee = IngestUtil.toBigInt(rmx.maxFeePerGas), // old pre EIP-1155
        tip = IngestUtil.toBigInt(rmx.maxPriorityFeePerGas), // old transactions without tip
        hash = m.hash,
        inp = rmx.input,
        non = IngestUtil.toBigInt(rmx.nonce),
        to = rmx.to,
        i = None,              // transaction index
        v = IngestUtil.toBigInt(rmx.v),
        typ = IngestUtil.toLong(rmx.`type`).map(_.toInt).getOrElse(0),
        chid = IngestUtil.toLong(rmx.chainId),
        sig = Some(s"${rmx.v}:${rmx.r}:${rmx.s}"),
        
        trace = trace
      )
    ).getOrElse(
      MempoolTx(
        ts = m.ts,
        pool = "0",     // NOTE: CHANGE TO Byte: pending - 0, queued - 1,
        bhash = None,         // blockhash
        b = None,               // blocknumber
        from = "",
        gas = 0L,
        p = BigInt(0),
        fee = None, // old pre EIP-1155
        tip = None, // old transactions without tip
        hash = m.hash,
        inp = None,
        non = BigInt(0),
        to = None,
        i = None,              // transaction index
        v = BigInt(0),
        typ = 0,
        chid = None,             // chainId
        sig = None, 
        
        trace = trace
      )
    )

    mtx
  }
}

class PipelineMempoolStreamCallTrace(config:Config) extends PipelineRpcMempoolWS[CallTrace](config) {

  def transform(m: MempoolTransaction): Seq[CallTrace] = {
    traceMempoolTx(m.hash)(config) 
  }
}

class PipelineMempoolStreamTxTrace(config:Config) extends PipelineRpcMempoolWS[MempoolTx](config) {

  def transform(m: MempoolTransaction): Seq[MempoolTx] = {    
    val trace = traceMempoolTx(m.hash)(config) 
    val mtx = getMempoolTx(m,Some(trace.toArray))    
    Seq(mtx)
  }
}

class PipelineMempoolStreamTx(config:Config) extends PipelineRpcMempoolWS[MempoolTx](config) {

  def transform(m: MempoolTransaction): Seq[MempoolTx] = {
    val mtx = getMempoolTx(m,None)
    Seq(mtx)
  }
}
