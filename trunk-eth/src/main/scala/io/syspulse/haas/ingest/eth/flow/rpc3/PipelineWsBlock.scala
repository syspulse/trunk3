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
import io.syspulse.haas.ingest.eth.BlockJson
import io.syspulse.haas.ingest.eth.BlockJson._

import io.syspulse.haas.core.RetryException

import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson._
import io.syspulse.haas.ingest.PipelineIngest

abstract class PipelineWsHead[E <: skel.Ingestable](config:Config)
  (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineFlowWS[RpcSubscriptionHeadResult,Block,E](config) {
  
  def apiSuffix():String = s"/stream.block"

  def decodeBlock(b:Block):Seq[Transaction] = {
    val blockHex = s"0x${b.i.toHexString}"
    val req = 
      s"""{
            "jsonrpc":"2.0","method":"eth_getBlockByNumber",
            "params":["${blockHex}",true],
            "id":0
          }""".trim.replaceAll("\\s+","")  

    val txxReceipts = try {
      val rsp = requests.post(config.rpcUrl, data = req,headers = Map("content-type" -> "application/json"))                        
      val body = rsp.text()
      
      rsp.statusCode match {
        case 200 => //
          log.trace(s"${body}")
        case _ => 
          // retry
          log.error(s"RPC error: ${rsp.statusCode}: ${body}")
          throw new RetryException(s"${rsp.statusCode}")
      }
                      
      val blocks = parseBlock(body)
      if(! blocks.isEmpty) {
        Some(decodeTransactions(blocks.head)(config,config.rpcUrl))
      } else 
        None

    } catch {
      case e:Exception =>
        log.error(s"failed to get batch: '${req}'",e)
        None
    }

    txxReceipts.map(_._1).getOrElse(Seq.empty)
  }

  def parse(data:String):Seq[RpcSubscriptionHeadResult] = {
    val h = parseHead(data)
    if(h.isDefined) {
      latestTs.set(IngestUtil.toLong(h.get.timestamp) * 1000L)
      Seq(h.get)
    } else 
      Seq.empty
  }

  def convert(b:RpcSubscriptionHeadResult):Block = {
    
    val blk = Block(
      IngestUtil.toLong(b.number),
      b.hash,
      b.parentHash,
      b.nonce,
      b.sha3Uncles,        
      b.logsBloom,
      b.transactionsRoot,
      b.stateRoot,        
      b.receiptsRoot,
      formatAddr(b.miner,config.formatAddr),
      
      IngestUtil.toBigInt(b.difficulty),
      IngestUtil.toBigInt(b.totalDifficulty).getOrElse(0L),
      0L, //IngestUtil.toLong(b.size), // size Unknown

      b.extraData, 
          
      IngestUtil.toLong(b.gasLimit), 
      IngestUtil.toLong(b.gasUsed), 
      IngestUtil.toLong(b.timestamp) * 1000L, 
      -1, //b.transactions.size,  // FIX ME ! transactions number need to be retrieved
      b.baseFeePerGas.map(d => IngestUtil.toLong(d)),
      
      // TODO: Add to Blocks !
      // b.withdrawalsRoot,
      // b.blobGasUsed,
      // b.blobGasPrice,
      // b.withdrawals
    )

    blk
  }

}

class PipelineWsBlock(config:Config) extends PipelineWsHead[Block](config) {

  def transform(b: Block): Seq[Block] = {
    Seq(b)
  }
}
