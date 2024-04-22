package io.syspulse.haas.intercept.server

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, JsonFormat, deserializationError}

import io.syspulse.skel.service.JsonCommon
import io.syspulse.haas.intercept.Script

object InterceptJson extends JsonCommon {
  
  import DefaultJsonProtocol._

  implicit val jf_scrpt = jsonFormat4(Script)
  
  implicit val jf_scrpts = jsonFormat2(Scripts)
  implicit val jf_scrpt_res = jsonFormat1(ScriptRes)
  implicit val jf_scrpt_CreateReq = jsonFormat2(ScriptCreateReq)
  implicit val jf_scrpt_UpdateReq = jsonFormat2(ScriptUpdateReq)
}
