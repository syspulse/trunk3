package io.syspulse.haas.serde

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

import io.syspulse.haas.core.Block

object BlockJson extends DefaultJsonProtocol {
  
  implicit val jf_block = jsonFormat19(Block.apply _)
}
