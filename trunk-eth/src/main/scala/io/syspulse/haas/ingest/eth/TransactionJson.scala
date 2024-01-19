package io.syspulse.haas.ingest.eth

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

object TransactionJson extends DefaultJsonProtocol {
  
  implicit val jf_transaction = jsonFormat20(Transaction.apply _)
}
