package io.syspulse.haas.ingest.solana

import com.typesafe.scalalogging.Logger

import io.syspulse.skel.service.JsonCommon
import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

object SolanaJson extends JsonCommon with NullOptions with ProductFormatsInstances {
  import DefaultJsonProtocol._

  implicit val jf_strk_tr = jsonFormat6(Transaction)
  implicit val jf_strk_block = jsonFormat6(Block)

  // implicit val jf_strk_ev = jsonFormat3(EventTx)
  // implicit val jf_strk_tx = jsonFormat15(Tx)
}
