package io.syspulse.haas.serde

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

import io.syspulse.haas.core.Transaction

object TransactionJson extends DefaultJsonProtocol {
  
  implicit val jf_transaction = jsonFormat20(Transaction.apply _)
}
