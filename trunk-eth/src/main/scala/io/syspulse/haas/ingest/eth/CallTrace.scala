package io.syspulse.haas.ingest.eth

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

import io.syspulse.haas.ingest.eth.flow.rpc3

case class CallTrace(
  ts:Long,
  hash:String,
  state:Option[rpc3.RpcTraceStates],
  call:Option[rpc3.RpcTraceCall],

) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)

  // override to show Array in a nice way
  override def toString = Util.toStringWithArray(this)
}
