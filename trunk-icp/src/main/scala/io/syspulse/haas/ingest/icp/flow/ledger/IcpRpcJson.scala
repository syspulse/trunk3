package io.syspulse.haas.ingest.icp.flow.ledger

import com.typesafe.scalalogging.Logger

// ATTENTION
import io.syspulse.skel.service.JsonCommon
import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

import io.syspulse.skel.Ingestable

// object IcpRpcJson extends DefaultJsonProtocol
object IcpRpcJson extends JsonCommon 
  //with NullOptions 
  with ProductFormatsInstances {
  import DefaultJsonProtocol._
  
  implicit val jf_icp_rpc_block = jsonFormat16(IcpRpcBlock)
  implicit val jf_icp_rpc_txs = jsonFormat2(IcpRpcTransactions)
    
}
