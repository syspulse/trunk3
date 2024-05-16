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
import io.syspulse.haas.ingest.vechain.Tx
import io.syspulse.haas.ingest.vechain.EventTx
import io.syspulse.haas.ingest.vechain.VechainJson._

import io.syspulse.haas.ingest.vechain.flow.rpc._
import io.syspulse.haas.ingest.vechain.flow.rpc.VechainRpcJson._
import io.syspulse.haas.ingest.vechain.VechainURI

abstract class PipelineVechainTx[E <: skel.Ingestable](config:Config)
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

class PipelineTx(config:Config) extends PipelineVechainTx[Tx](config) {    
  val rpcUri = VechainURI(config.feed,apiToken = config.apiToken)
  val uri = rpcUri.uri
  
  def transform(block: RpcBlock): Seq[Tx] = {

    val tt = parseBatchTx(block.number)(config,uri)

    val clausesNum = tt.foldLeft(0)((n,t) => n + t.clauses.size)
    log.info(s"Block[${block.number},${tt.size},${clausesNum}]")
        
    // ATTENTION !!!
    // For Clauses with muliple contract calls Events will be propagated to every Transaction/Clause !!
    // There is no way to distringuish in VeChain which Events are caused by which Clause
    val txx = tt.map( tx => {      
        tx.clauses.map(clause => Tx(
          ts = block.timestamp,
          b = block.number,
          hash = tx.id,
          sz = tx.size,

          from = tx.origin,
          to = clause.to, 
          v = toBigInt(clause.value),//new java.math.BigInteger(clause.value.stripPrefix("0x"),16),        // value
          nonce = tx.nonce,
          
          gas = tx.gas, 
          pric = tx.gasPriceCoef, 
          
          data = clause.data,

          exp = tx.expiration,
          del = tx.delegator,
          dep = tx.dependsOn,

          used = tx.gasUsed,
          pay = tx.gasPayer,
          paid = toBigInt(tx.paid),
          rwd = toBigInt(tx.reward),
          fail = tx.reverted,
          
          logs = {
            if(clause.data == "0x" || clause.data == "")
              Array[EventTx]()
            else { 
              val ll:Seq[EventTx] = tx.outputs.flatMap( o => o.events.map( e => {
                EventTx(
                  o.contractAddress.getOrElse(""),
                  e.data,
                  e.topics.toArray
                )                
              }))
              
              ll.toArray
            }
          }
        ))
    })
  
    // commit cursor
    cursor.commit(block.number)
    
    txx.flatten
  }
}