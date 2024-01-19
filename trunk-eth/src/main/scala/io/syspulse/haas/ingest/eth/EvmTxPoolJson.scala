package io.syspulse.haas.ingest.eth

import io.syspulse.skel.service.JsonCommon

import spray.json.DefaultJsonProtocol
import spray.json.{DeserializationException, JsString, JsValue, JsonFormat, deserializationError}
import spray.json.NullOptions

// with NullOptions only for writing
object EvmTxPoolJson extends JsonCommon {
  implicit val jf_EvmAL = jsonFormat2(EvmAccessList)
  implicit val jf_EvmTx = jsonFormat21(EvmTx)
  implicit val jf_EvmTxRaw = jsonFormat19(EvmTxRaw)
  implicit val jf_EvmTxPoolResult = jsonFormat2(EvmTxPoolResult)
  implicit val jf_EvmTxPool = jsonFormat3(EvmTxPool)
}

