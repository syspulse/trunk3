package io.haas.ingest.ext

import io.syspulse.skel.Ingestable

case class MempoolNone(  

) extends Ingestable {
  override def getKey:Option[Any] = None
}
