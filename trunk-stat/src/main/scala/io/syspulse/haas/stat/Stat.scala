package io.syspulse.haas.stat

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

case class Stat(
  ts:Long,
  countBlock:Long,
  countTx:Long,  
  block:Long,
  tx:Long,

) extends Ingestable {
  override def getKey:Option[Any] = Some(ts)   
}
