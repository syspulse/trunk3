package io.haas.ingest.solana.flow.rpc

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

  implicit val jf_rpc_parsed_inst: RootJsonFormat[RpcParsedInstruction] = new RootJsonFormat[RpcParsedInstruction] {
    override def write(p: RpcParsedInstruction): JsValue = {
      val fields = scala.collection.mutable.LinkedHashMap.empty[String, JsValue]
      fields += "type" -> JsString(p.`type`)
      p.info.foreach(v => fields += "info" -> v)
      JsObject(fields.toMap)
    }

    override def read(value: JsValue): RpcParsedInstruction = {
      val obj = value.asJsObject
      val info = obj.fields.get("info") match {
        case Some(v: JsObject) => Some(v)
        case Some(JsNull)      => None
        case _                 => None
      }
      val tpe = obj.fields.get("type").collect { case JsString(s) => s }.getOrElse("")
      RpcParsedInstruction(info = info, `type` = tpe)
    }
  }

  implicit val jf_rpc_inst: RootJsonFormat[RpcInstruction] = new RootJsonFormat[RpcInstruction] {
    override def write(i: RpcInstruction): JsValue = {
      val fields = scala.collection.mutable.LinkedHashMap.empty[String, JsValue]

      // required-ish field
      fields += "accounts" -> JsArray(i.accounts.map(JsString(_)).toVector)

      i.data.foreach(v => fields += "data" -> JsString(v))
      i.programIdIndex.foreach(v => fields += "programIdIndex" -> JsNumber(v))
      i.programId.foreach(v => fields += "programId" -> JsString(v))
      i.program.foreach(v => fields += "program" -> JsString(v))
      i.parsed.foreach(v => fields += "parsed" -> v.toJson)
      i.stackHeight.foreach(v => fields += "stackHeight" -> JsNumber(v))

      JsObject(fields.toMap)
    }

    override def read(value: JsValue): RpcInstruction = {
      val obj = value.asJsObject

      val accounts: Seq[String] = obj.fields.get("accounts") match {
        case Some(JsArray(values)) =>
          values.map {
            case JsString(s) => s
            case JsNumber(n) => n.toString
            case JsObject(fields) =>
              fields.get("pubkey").collect { case JsString(s) => s }.getOrElse(JsObject(fields).compactPrint)
            case other => other.compactPrint
          }
        case _ => Seq.empty
      }

      RpcInstruction(
        accounts = accounts,
        data = obj.fields.get("data") match {
          case Some(JsString(s)) => Some(s)
          case _ => None
        },
        programIdIndex = obj.fields.get("programIdIndex") match {
          case Some(JsNumber(n)) => Some(n.toLong)
          case _ => None
        },
        programId = obj.fields.get("programId") match {
          case Some(JsString(s)) => Some(s)
          case _ => None
        },
        program = obj.fields.get("program") match {
          case Some(JsString(s)) => Some(s)
          case _ => None
        },
        parsed = obj.fields.get("parsed") match {
          case Some(v: JsObject) => Some(v.convertTo[RpcParsedInstruction])
          case _ => None
        },
        stackHeight = obj.fields.get("stackHeight") match {
          case Some(JsNumber(n)) => Some(n.toLong)
          case _ => None
        }
      )
    }
  }

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
