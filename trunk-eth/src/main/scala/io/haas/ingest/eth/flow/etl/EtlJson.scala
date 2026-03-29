package io.haas.ingest.eth.flow.etl

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

import io.haas.ingest.eth.BlockJson
import io.haas.ingest.eth.LogJson
import io.syspulse.skel.service.JsonCommon

import io.haas.ingest.ext.IngestExtJson
import io.haas.ingest.ext.{Block, Log, Tx}

object EtlJson extends DefaultJsonProtocol { //JsonCommon {
  implicit val jf_etl_block = jsonFormat19(Block.apply _)  
  implicit val jf_etl_logtx = jsonFormat4(Log.apply _)  
  implicit val jf_etl_tx = jsonFormat21(Tx.apply _)    
}
