package io.syspulse.haas.ingest.stellar

import com.typesafe.scalalogging.Logger

import io.syspulse.skel.service.JsonCommon
import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

import io.syspulse.skel.Ingestable

object StellarJson extends DefaultJsonProtocol
  //extends JsonCommon  with ProductFormatsInstances 
{
  //implicit val jf_icp_op = jsonFormat7(Operation)
  implicit val jf_st_tx = jsonFormat9(Tx)
  implicit val jf_st_tr = jsonFormat18(Transaction)
  implicit val jf_st_block = jsonFormat5(Block)   
}
