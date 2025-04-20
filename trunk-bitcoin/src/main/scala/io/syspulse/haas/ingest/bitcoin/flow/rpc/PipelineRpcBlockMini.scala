package io.syspulse.haas.ingest.bitcoin.flow.rpc

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import scala.util.{Try,Success,Failure}
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

import io.syspulse.skel.serde.ParqIgnore
import com.github.mjakubowski84.parquet4s.{ParquetRecordEncoder,ParquetSchemaResolver}

import io.syspulse.haas.ingest.bitcoin.flow.rpc.RpcBlock
import io.syspulse.haas.ingest.bitcoin.flow.rpc.RpcJsonProtocol._
import io.syspulse.haas.ingest.bitcoin.{Block}
import io.syspulse.haas.ingest.bitcoin.BitcoinJson._

import java.util.concurrent.TimeUnit

import io.syspulse.haas.ingest.Config

import io.syspulse.skel.serde.Parq._

object ParqRpcScriptPubKey extends ParqIgnore[RpcScriptPubKey]
object ParqRpcScriptSig extends ParqIgnore[RpcScriptSig]
object ParqRpcTransactionInput extends ParqIgnore[RpcTransactionInput]
object ParqRpcTransactionOutput extends ParqIgnore[RpcTransactionOutput]
object ParqRpcTransaction extends ParqIgnore[RpcTransaction]
object ParqRpcBlock extends ParqIgnore[RpcBlock]

import ParqRpcBlock._ 
import ParqRpcTransaction._
import ParqRpcTransactionInput._
import ParqRpcTransactionOutput._
import ParqRpcScriptPubKey._
import ParqRpcScriptSig._


class PipelineRpcBlockMini(config:Config) extends PipelineRPC[RpcBlock,RpcBlock,RpcBlock](config) {  

  def apiSuffix():String = s"/block"

  def parse(data:String):Seq[RpcBlock] = {
    rpc.parseBlock(data) match {
      case Success(b) => 
        Seq(b)
      case Failure(e) => 
        log.error(s"failed to parse block: ${e.getMessage}")
        Seq.empty
    }    
  }

  def convert(block:RpcBlock):RpcBlock = {
    block
  }

  def transform(block: RpcBlock): Seq[RpcBlock] = {
    val outBlock = block.copy(
      tx = block.tx.map(tx => tx.copy(
        vin = tx.vin.map(vin => vin.copy(
          txinwitness = Some(Seq("0x"))
        ))
      ))
    )

    Seq(outBlock)
  }    
}
