package io.syspulse.haas.intercept

import scala.jdk.CollectionConverters._
import io.syspulse.skel.Ingestable

case class Intercept(
  res:String,                   // results
  chain:Option[String] = None  // chain_id 
) extends Ingestable {
}
