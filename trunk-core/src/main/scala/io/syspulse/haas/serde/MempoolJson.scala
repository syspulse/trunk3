package io.syspulse.haas.serde

import io.syspulse.skel.service.JsonCommon

import spray.json.DefaultJsonProtocol
import spray.json.{DeserializationException, JsString, JsValue, JsonFormat, deserializationError}
import spray.json.NullOptions

import io.syspulse.haas.core.MempoolTx

// with NullOptions only for writing
object MempoolJson extends JsonCommon {
  implicit val jf_MempoolTx = jsonFormat14(MempoolTx)
}

