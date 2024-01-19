package io.syspulse.haas.ingest.vechain.flow.rpc

import com.typesafe.scalalogging.Logger

// ATTENTION
import io.syspulse.skel.service.JsonCommon
import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

object VechainRpcJson extends JsonCommon {
  
  implicit val jf_vch_tx_meta = jsonFormat3(RpcTxMeta)
  implicit val jf_vch_clause = jsonFormat3(RpcClause)
  implicit val jf_vch_tx = jsonFormat13(RpcTx)
  
  implicit val jf_vch_blk = jsonFormat18(RpcBlock)
  implicit val jf_vch_blk_tx = jsonFormat18(RpcBlockTx)
}
