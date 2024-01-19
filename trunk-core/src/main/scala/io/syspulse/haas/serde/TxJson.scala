package io.syspulse.haas.serde

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

import io.syspulse.haas.core.Tx

object TxJson extends DefaultJsonProtocol {
  import BlockJson._
  import EventJson._
  implicit val jf_tx = jsonFormat20(Tx.apply _)
}
