package io.syspulse.haas.ingest.solana.flow.rpc

import com.typesafe.scalalogging.Logger

// ATTENTION
import io.syspulse.skel.service.JsonCommon
import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

object SolanaRpcJson extends JsonCommon {
  // implicit val jf_rpc_inst = jsonFormat4(RpcInstruction)
  // implicit val jf_rpc_iinst = jsonFormat2(RpcInnerInstruction)

  implicit val jf_rpc_addr = jsonFormat2(RpcLoadedAddresses)
  implicit val jf_rpc_rew = jsonFormat5(RpcReward)
  implicit val jf_rpc_st = jsonFormat2(RpcStatus)
  implicit val jf_rpc_inst = jsonFormat4(RpcInstruction)
  implicit val jf_rpc_iinst = jsonFormat2(RpcInnerInstruction)
  
  implicit val jf_rpc_ui = jsonFormat4(RpcUiTokenAmount)
  implicit val jf_rpc_tok_bal = jsonFormat5(RpcPostTokenBalance)

  implicit val jf_rpc_err = jsonFormat1(RpcErr)

  implicit val jf_rpc_meta = jsonFormat11(RpcMeta)

  implicit val jf_rpc_head = jsonFormat3(RpcHeader)
  
  implicit val jf_rpc_msg = jsonFormat4(RpcMessage) 

  implicit val jf_rpc_tx_tx = jsonFormat2(RpcTransactionTx) 
  implicit val jf_rpc_tx = jsonFormat3(RpcTransaction) 
  implicit val jf_rpc_blk = jsonFormat6(RpcBlock)   

  implicit val jf_rpc_blk_res = jsonFormat3(RpcBlockResult)   
  
}
