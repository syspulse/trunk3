package io.syspulse.haas.ingest.eth

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

import io.syspulse.haas.ingest.eth.BlockJson
import io.syspulse.haas.ingest.eth.EventJson

object CallTraceJson extends DefaultJsonProtocol {
  implicit val jf_call_trc = jsonFormat4(CallTrace.apply _)  
}
