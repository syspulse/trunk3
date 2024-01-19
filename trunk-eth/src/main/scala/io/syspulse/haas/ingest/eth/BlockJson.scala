package io.syspulse.haas.ingest.eth

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

object BlockJson extends DefaultJsonProtocol {
  
  implicit val jf_block = jsonFormat19(Block.apply _)
}
