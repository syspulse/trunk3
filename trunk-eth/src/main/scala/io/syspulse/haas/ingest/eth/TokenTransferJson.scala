package io.syspulse.haas.ingest.eth

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

object TokenTransferJson extends DefaultJsonProtocol {
  
  implicit val jf_tokentransfer = jsonFormat8(TokenTransfer.apply _)
}
