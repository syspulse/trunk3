package io.syspulse.haas.ingest.starknet.flow.rpc

import com.typesafe.scalalogging.Logger

// ATTENTION
import io.syspulse.skel.service.JsonCommon
import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

object StarknetRpcJson extends JsonCommon {
  
  implicit val jf_rpc_tx = jsonFormat11(RpcTx)
  implicit val jf_rpc_l1_gas = jsonFormat1(RpcL1Gas)
  implicit val jf_rpc_res = jsonFormat9(RpcBlock)
  implicit val jf_rpc_bl = jsonFormat3(RpcBlockResult)

  implicit val jf_rpc_evn = jsonFormat3(RpcEvent)
  implicit val jf_rpc_evn_r = jsonFormat4(RpcEvents)
  implicit val jf_rpc_evns = jsonFormat3(RpcEventsResult) 

  implicit val jf_rpc_recp_msg = jsonFormat3(RcpReceiptMessageL1) 
  implicit val jf_rpc_recp = jsonFormat9(RpcReceipt) 
  implicit val jf_rpc_recp_res = jsonFormat3(RpcReceiptResult)   
  implicit val jf_rpc_recp_batch_res = jsonFormat3(RpcBlockReceiptsResult)  
}
