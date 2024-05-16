package io.syspulse.haas.ingest.vechain

import com.typesafe.scalalogging.Logger

import io.syspulse.skel.service.JsonCommon
import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

object VechainJson extends DefaultJsonProtocol 
  //extends JsonCommon with ProductFormatsInstances 
{  
  import DefaultJsonProtocol._

  implicit val jf_vc_ev = jsonFormat3(EventTx)
  implicit val jf_vc_tx = jsonFormat21(Tx)
  implicit val jf_vc_tr = jsonFormat16(Transaction)
  implicit val jf_vc_block = jsonFormat18(Block)
}
