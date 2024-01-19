package io.syspulse.haas.ingest.icp.flow.rosetta

import com.typesafe.scalalogging.Logger

// ATTENTION
import io.syspulse.skel.service.JsonCommon
import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

import io.syspulse.skel.Ingestable

object IcpRpcJson extends JsonCommon with NullOptions with ProductFormatsInstances {
  import DefaultJsonProtocol._

  implicit val jf_icp_rpc_b_id = jsonFormat2(IcpRpcBlockIdentifier)
  implicit val jf_icp_rpc_t_id = jsonFormat1(IcpRpcTransactionIdentifier)
  implicit val jf_icp_rpc_t_op_id = jsonFormat1(IcpRpcTransactionOperationIdentifier)
  implicit val jf_icp_rpc_acc = jsonFormat1(IcpRpcAccount)
  implicit val jf_icp_rpc_curr = jsonFormat2(IcpRpcCurrency)
  implicit val jf_icp_rpc_amount = jsonFormat2(IcpRpcAmount)
  implicit val jf_icp_rpc_meta = jsonFormat3(IcpRpcMetadata)
  implicit val jf_icp_rpc_t_op = jsonFormat5(IcpRpcTransactionOperation)  

  implicit val jf_icp_rpc_t = jsonFormat3(IcpRpcTransaction) 
  implicit val jf_icp_rpc_b = jsonFormat4(IcpRpcBlockBlock)
  implicit val jf_icp_rpc_block = jsonFormat1(IcpRpcBlock)
    
}
