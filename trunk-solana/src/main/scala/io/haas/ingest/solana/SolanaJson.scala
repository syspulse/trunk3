package io.haas.ingest.solana

import com.typesafe.scalalogging.Logger

import io.syspulse.skel.service.JsonCommon
import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

import io.haas.ingest.solana.flow.rpc.SolanaRpcJson._

object SolanaJson extends DefaultJsonProtocol with NullOptions
  //extends JsonCommon with ProductFormatsInstances 
{  
  import DefaultJsonProtocol._  

  implicit val jf_strk_tr: RootJsonFormat[Transaction] = jsonFormat12(Transaction)
  implicit val jf_strk_block: RootJsonFormat[Block] = jsonFormat6(Block)
}
