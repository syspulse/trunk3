package io.haas.ingest.eth

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

import io.haas.ingest.eth.BlockJson
import io.haas.ingest.eth.LogJson

object TxJson extends DefaultJsonProtocol {
  import BlockJson._
  import LogJson._
  implicit val jf_tx = jsonFormat21(Tx.apply _)  
}
