package io.syspulse.haas.intercept.script

import scala.util.Random
import io.jvm.uuid._
import scala.util.{Success,Failure,Try}
import com.typesafe.scalalogging.Logger


case class Script(
  name:String,
  src:String,
  typ:String = "js",
  //ts:Long = System.currentTimeMillis(),
) {
  override def toString = s"Script(${name},${src.take(16)}...,${typ})"
}

object Script {
  type ID = String //UUID
}
