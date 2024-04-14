package io.syspulse.haas.stat

import scala.jdk.CollectionConverters._

import spray.json._
import DefaultJsonProtocol._

object StatJson extends DefaultJsonProtocol {
  implicit val jf_stat = jsonFormat5(Stat.apply _)  
}
