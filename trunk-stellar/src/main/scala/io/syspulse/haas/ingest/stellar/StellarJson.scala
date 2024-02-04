package io.syspulse.haas.ingest.stellar

import com.typesafe.scalalogging.Logger

import io.syspulse.skel.service.JsonCommon
import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

import io.syspulse.skel.Ingestable

object StellarJson extends JsonCommon with NullOptions with ProductFormatsInstances {
  import DefaultJsonProtocol._
  //implicit val jf_icp_op = jsonFormat7(Operation)
  implicit val jf_st_tx = jsonFormat8(Transaction)
  implicit val jf_st_block = jsonFormat5(Block)   
}
