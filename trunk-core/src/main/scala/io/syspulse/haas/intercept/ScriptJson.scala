package io.syspulse.haas.intercept

import spray.json._
import DefaultJsonProtocol._
import io.syspulse.skel.service.JsonCommon

object ScriptJson extends JsonCommon with NullOptions {
  
  implicit val jf_scr_1 = jsonFormat6(Script.apply _)
  
}

