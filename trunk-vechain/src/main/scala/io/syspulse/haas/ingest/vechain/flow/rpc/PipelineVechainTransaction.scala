package io.syspulse.haas.ingest.vechain.flow.rpc

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

import akka.util.ByteString

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

import io.syspulse.haas.core.RetryException

import io.syspulse.haas.ingest.vechain.Block
import io.syspulse.haas.ingest.vechain.Transaction
import io.syspulse.haas.ingest.vechain.VechainJson._

import io.syspulse.haas.ingest.vechain.flow.rpc._
import io.syspulse.haas.ingest.vechain.flow.rpc.VechainRpcJson._
import io.syspulse.haas.ingest.vechain.VechainURI

abstract class PipelineVechainTransaction[E <: skel.Ingestable](config:Config)
                                                     (implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  PipelineVechain[RpcBlock,RpcBlock,E](config) {
    
  def apiSuffix():String = s"/transaction"

  def parse(data:String):Seq[RpcBlock] = {
    val bb:Seq[RpcBlock] = parseBlock(data)    
   
    // ignore pending transactions
    if(bb.size!=0) {
      latestTs.set(bb.last.timestamp * 1000L)
      bb
    } else
      Seq()
  }

  def convert(b:RpcBlock):RpcBlock = {
    b
  }

  // def transform(tx: Transaction): Seq[Transaction] = {
  //   Seq(tx)
  // }
}

class PipelineTransaction(config:Config) extends PipelineVechainTransaction[Transaction](config) {    
  val rpcUri = VechainURI(config.feed,apiToken = config.apiToken)
  val uri = rpcUri.uri
  
  def transform(block: RpcBlock): Seq[Transaction] = {

    val tt = 
    // {
    //   block.transactions.flatMap( txHash => {
    //     Seq(single(txHash)(config))
    //   })      
    // }
    {
      parseBatchTx(block.number)(config,uri)
    }
    
    val txx = tt.map( tx => {      
        tx.clauses.map(clause => Transaction(
          ts = block.timestamp * 1000L,
          b = block.number,
          hash = tx.id,
          sz = tx.size,

          from = tx.origin,
          to = clause.to, 
          v = new java.math.BigInteger(clause.value.stripPrefix("0x"),16),        // value
          nonce = tx.nonce,
          
          gas = tx.gas, 
          pric = tx.gasPriceCoef, 
          
          data = clause.data,

          blk = tx.blockRef,
          exp = tx.expiration,
          del = tx.delegator,
          dep = tx.dependsOn
        ))
    })
  
    // commit cursor
    cursor.commit(block.number)
    
    txx.flatten
  }
}