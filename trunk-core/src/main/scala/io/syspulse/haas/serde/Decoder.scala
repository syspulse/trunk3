package io.syspulse.haas.serde

import scala.jdk.CollectionConverters._
import scala.util.Random

trait Decoder[T] {  
  def parse(data:String):Seq[T]
}
