package io.syspulse.haas.ingest

import java.util.concurrent.atomic.AtomicLong
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

import io.syspulse.skel.util.Util

object IngestUtil {

  def toLong(data:String) = java.lang.Long.parseLong(data.stripPrefix("0x"),16)
  def toBigInt(data:String) = Util.toBigInt(data) //BigInt(Util.unhex(data))
  def toOption(data:String) = if(data.isEmpty() || data=="0x") None else Some(data)
  def toOptionLong(data:String) = if(data.isEmpty() || data=="0x") None else Some(toLong(data))
  
}
