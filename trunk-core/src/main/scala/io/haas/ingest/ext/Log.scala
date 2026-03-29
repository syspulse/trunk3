package io.haas.ingest.ext

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class Log(
  index:Int,
  address:String,
  data:String,  
  topics:Array[String] = Array(), 
) extends Ingestable {
  
  override def toString = Util.toStringWithArray(this)
}
