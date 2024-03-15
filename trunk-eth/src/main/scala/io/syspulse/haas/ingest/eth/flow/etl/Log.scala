package io.syspulse.haas.ingest.eth.etl

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

// used only in Fat Tx
case class LogTx(
  index:Int,
  address:String,
  data:String,  
  topics:Array[String] = Array(), 
) extends Ingestable {
  
  // override to show Array in a nice way
  override def toString = Util.toStringWithArray(this)
}
