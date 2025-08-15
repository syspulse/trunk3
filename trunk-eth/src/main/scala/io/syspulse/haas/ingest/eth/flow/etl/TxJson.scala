package io.syspulse.haas.ingest.eth.etl

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

import io.syspulse.haas.ingest.eth.BlockJson
import io.syspulse.haas.ingest.eth.EventJson
import io.syspulse.skel.service.JsonCommon

object TxJson extends DefaultJsonProtocol { //JsonCommon {
  implicit val jf_etl_block = jsonFormat19(Block.apply _)  
  implicit val jf_etl_logtx = jsonFormat4(LogTx.apply _)  
  implicit val jf_etl_tx = jsonFormat21(Tx.apply _)  
}
