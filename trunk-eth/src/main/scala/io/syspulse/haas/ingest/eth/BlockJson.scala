package io.syspulse.haas.ingest.eth

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

object BlockJson extends DefaultJsonProtocol {  
  import TransactionJson._

  implicit val jf_blk_block = jsonFormat20(Block.apply _)
}
