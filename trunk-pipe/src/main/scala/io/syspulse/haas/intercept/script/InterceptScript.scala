package io.syspulse.haas.intercept.script

import scala.util.Random
import io.jvm.uuid._
import scala.util.{Success,Failure,Try}
import com.typesafe.scalalogging.Logger

case class InterceptScript(
  src:String,
  typ:String = "js",  
) {
  override def toString = s"InterceptScript(${typ},'${src.take(16)}...')"
}
