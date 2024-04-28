package io.syspulse.haas.intercept.server

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, JsonFormat, deserializationError}

import io.syspulse.skel.service.JsonCommon

import io.syspulse.haas.intercept.Intercept
import io.syspulse.haas.intercept.InterceptResult
import io.syspulse.haas.intercept.Script
import io.syspulse.haas.intercept.ScriptJson

object InterceptJson extends JsonCommon {
  
  import DefaultJsonProtocol._
  import ScriptJson._  
  
  implicit val jf_scrpts = jsonFormat2(Scripts)
  implicit val jf_scrpt_res = jsonFormat1(ScriptRes)
  implicit val jf_scrpt_CreateReq = jsonFormat2(ScriptCreateReq)
  implicit val jf_scrpt_UpdateReq = jsonFormat2(ScriptUpdateReq)

  implicit val jf_ix_1 = jsonFormat2(Intercept.apply _)
  implicit val jf_ix_res_1 = jsonFormat3(InterceptResult.apply _)
}
