package io.syspulse.haas.ingest.eth

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class CallTrace(
  ts:Long,
  hash:String,
  state:String,
  calls:String,
) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)

  // override to show Array in a nice way
  override def toString = Util.toStringWithArray(this)
}
