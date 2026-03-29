package io.haas.ingest.ext

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

import io.syspulse.skel.service.JsonCommon

object IngestExtJson extends DefaultJsonProtocol { //JsonCommon {
  implicit val jf_ext_block = jsonFormat19(Block.apply _)  
  implicit val jf_etl_logtx = jsonFormat4(Log.apply _)  
  implicit val jf_etl_tx = jsonFormat21(Tx.apply _)  
}
