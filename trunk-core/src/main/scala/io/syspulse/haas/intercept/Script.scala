package io.syspulse.haas.intercept

import scala.collection.immutable

import io.jvm.uuid._

final case class Script(  
  id:String,  
  src:String,  
  ts0:Long,
  ref:String = "src://",        // reference to source (src://, file://, ref://)
  typ:String = "js",             // js
  ts:Long = System.currentTimeMillis()
) {
  override def toString = s"Script(${id},${typ},${ref},'${src.take(16)}...',${ts})"
}
