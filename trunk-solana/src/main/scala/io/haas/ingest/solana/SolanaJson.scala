package io.haas.ingest.solana

import com.typesafe.scalalogging.Logger

import io.syspulse.skel.service.JsonCommon
import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

import io.haas.ingest.solana.flow.rpc.SolanaRpcJson._

object SolanaJson extends DefaultJsonProtocol
  //extends JsonCommon with ProductFormatsInstances 
{  
  import DefaultJsonProtocol._  

  implicit val jf_tok_ui: RootJsonFormat[TokUI] = jsonFormat4(TokUI)
  implicit val jf_tok_bal: RootJsonFormat[TokBal] = jsonFormat5(TokBal)
  
  implicit val jf_sol_rew: RootJsonFormat[Reward] = jsonFormat5(Reward)
  implicit val jf_strk_tr: RootJsonFormat[Transaction] = jsonFormat18(Transaction)
  implicit val jf_strk_block: RootJsonFormat[Block] = jsonFormat7(Block)

  implicit val jf_sol_tx: RootJsonFormat[Tx] = jsonFormat16(Tx)
}
