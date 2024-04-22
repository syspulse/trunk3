package io.syspulse.haas.intercept

import scala.collection.immutable

import io.jvm.uuid._

final case class Script(  
  id:String,
  src:String,
  ts0:Long = System.currentTimeMillis(),
  ts:Long = System.currentTimeMillis()
)
