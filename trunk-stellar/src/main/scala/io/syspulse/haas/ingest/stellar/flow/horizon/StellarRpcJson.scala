package io.syspulse.haas.ingest.stellar.flow.horizon

import com.typesafe.scalalogging.Logger

// ATTENTION
import io.syspulse.skel.service.JsonCommon
import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

import io.syspulse.skel.Ingestable

object StellarRpcJson extends JsonCommon with NullOptions with ProductFormatsInstances {
  import DefaultJsonProtocol._
  
  implicit val jf_st_rpc_root = jsonFormat11(StellarRpcRoot)
  implicit val jf_st_rpc_block = jsonFormat17(StellarRpcBlock)
  
  implicit val jf_st_rpc_tb = jsonFormat1(StellarRpcTimebounds)    
  implicit val jf_st_rpc_prec = jsonFormat2(StellarRpcPrecondition)    
  implicit val jf_st_rpc_tx = jsonFormat20(StellarRpcTransaction)

  implicit val jf_st_rpc_emb = jsonFormat1(StellarRpcEmbedded)    
  implicit val jf_st_rpc_txx = jsonFormat1(StellarRpcTransactions)    
}
