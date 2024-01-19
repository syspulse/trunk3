package io.syspulse.haas.ingest.eth

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

import io.syspulse.haas.ingest.eth.BlockJson
import io.syspulse.haas.ingest.eth.EventJson

object TxJson extends DefaultJsonProtocol {
  import BlockJson._
  import EventJson._
  implicit val jf_tx = jsonFormat20(Tx.apply _)
}
