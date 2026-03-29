package io.haas.ingest.eth

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

object TransactionJson extends DefaultJsonProtocol {
  import LogJson._
  implicit val jf_transaction = jsonFormat21(Transaction.apply _)
}
