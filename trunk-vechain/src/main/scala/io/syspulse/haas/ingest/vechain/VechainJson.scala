package io.syspulse.haas.ingest.vechain

import com.typesafe.scalalogging.Logger

import io.syspulse.skel.service.JsonCommon
import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

object VechainJson extends JsonCommon with NullOptions with ProductFormatsInstances {
  import DefaultJsonProtocol._

  implicit val jf_strk_tx = jsonFormat16(Transaction)
  implicit val jf_strk_block = jsonFormat18(Block)
}
