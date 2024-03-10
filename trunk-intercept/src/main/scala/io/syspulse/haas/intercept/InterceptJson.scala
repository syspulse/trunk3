package io.syspulse.haas.intercept

import spray.json._
import DefaultJsonProtocol._
import io.syspulse.skel.service.JsonCommon

object InterceptJson extends JsonCommon with NullOptions {
  
  implicit val jf_int_1 = jsonFormat2(Intercept.apply _)
  
}

