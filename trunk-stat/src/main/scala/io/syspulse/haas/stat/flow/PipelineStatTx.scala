package io.syspulse.haas.stat.flow

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

import io.syspulse.haas.ingest.eth.etl.TxJson
import io.syspulse.haas.ingest.eth.etl.Tx

import io.syspulse.haas.ingest.PipelineIngest

import io.syspulse.haas.stat.Config
import io.syspulse.haas.stat.Stat
import io.syspulse.haas.stat.StatJson._


abstract class PipelineStatTx[E <: skel.Ingestable](config:Config)(implicit val fmtE:JsonFormat[E],parqEncoders:ParquetRecordEncoder[E],parsResolver:ParquetSchemaResolver[E]) extends 
  Pipeline[Tx,Stat,E](config.feed,config.output,config.throttle,config.delimiter,config.buffer,format=config.format) 
  //PipelineIngest[Tx,Tx,E](config) 
  with PipelineStat[E] {
  
  def apiSuffix():String = s"/stat/tx"

  var countTx:Long = 0
  var countBlock:Long = 0
  var lastBlock:Long = 0

  def parse(data:String):Seq[Tx] = {
    val d = parseTx(data)
    d
  }
  
  def process:Flow[Tx,Stat,_] = Flow[Tx].map(tx => {
    countTx = countTx + 1
    if(tx.block.number > lastBlock) {
      lastBlock = tx.block.number
      countBlock = countBlock + 1
    }

    Stat(
      tx.block.timestamp,
      countBlock,
      countTx
    )
  })
}

class PipelineTx(config:Config) extends PipelineStatTx[Stat](config) {

  def transform(stat: Stat): Seq[Stat] = Seq(stat)
}
