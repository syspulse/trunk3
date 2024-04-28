package io.syspulse.haas.intercept

import scala.collection.immutable

import io.jvm.uuid._

final case class Script(  
  id:String,
  src:String,
  ref:String = "src://",        // reference to source (src://, file://, ref://)
  ts0:Long,
  ts:Long = System.currentTimeMillis()
)
