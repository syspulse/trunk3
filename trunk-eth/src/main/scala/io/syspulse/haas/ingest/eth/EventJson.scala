package io.syspulse.haas.ingest.eth

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

object EventJson extends DefaultJsonProtocol {
  
  implicit val jf_event = jsonFormat8(Event.apply _)
  implicit val jf_tx_event = jsonFormat4(EventTx.apply _)
}
