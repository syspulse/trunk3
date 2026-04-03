package io.haas.ingest.ext

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

import io.syspulse.skel.service.JsonCommon

object IngestExtJson extends DefaultJsonProtocol with NullOptions { //JsonCommon {
  import DefaultJsonProtocol._ 

  implicit val jf_ext_block:RootJsonFormat[Block] = jsonFormat20(Block)  
  implicit val jf_etl_logtx:RootJsonFormat[Log] = jsonFormat4(Log)  
  implicit val jf_etl_tx:RootJsonFormat[Tx] = jsonFormat21(Tx)  
}
