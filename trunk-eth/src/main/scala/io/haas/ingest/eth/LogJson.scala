package io.haas.ingest.eth

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

object LogJson extends DefaultJsonProtocol {
  
  implicit val jf_log = jsonFormat8(Log.apply _)
  implicit val jf_tx_log = jsonFormat4(LogTx.apply _)
}
