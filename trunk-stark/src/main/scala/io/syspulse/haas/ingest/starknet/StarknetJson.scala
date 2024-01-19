package io.syspulse.haas.ingest.starknet

import com.typesafe.scalalogging.Logger

import io.syspulse.skel.service.JsonCommon
import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

object StarknetJson extends JsonCommon with NullOptions with ProductFormatsInstances {
  import DefaultJsonProtocol._

  implicit val jf_strk_tx = jsonFormat12(Transaction)
  implicit val jf_strk_block = jsonFormat9(Block)
}
