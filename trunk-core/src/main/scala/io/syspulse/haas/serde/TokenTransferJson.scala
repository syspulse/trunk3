package io.syspulse.haas.serde

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

import io.syspulse.haas.core.TokenTransfer

object TokenTransferJson extends DefaultJsonProtocol {
  
  implicit val jf_tokentransfer = jsonFormat8(TokenTransfer.apply _)
}
