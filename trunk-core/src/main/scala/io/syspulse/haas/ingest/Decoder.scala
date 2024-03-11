package io.syspulse.haas.ingest

import java.util.concurrent.atomic.AtomicLong
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util
import io.syspulse.skel.config._

import io.syspulse.skel.ingest._

import spray.json._
import DefaultJsonProtocol._
import java.util.concurrent.TimeUnit

import io.syspulse.haas.serde._

trait Decoder[E,BLOCK,TRANSACTION,TRANSFER,LOG,TX] {
  implicit val fmt:JsonFormat[E]

  def toLong(data:String) = java.lang.Long.parseLong(data.stripPrefix("0x"),16)
  def toBigInt(data:String) = Util.toBigInt(data) //BigInt(Util.unhex(data))
  def toOption(data:String) = if(data.isEmpty() || data=="0x") None else Some(data)
  def toOptionLong(data:String) = if(data.isEmpty() || data=="0x") None else Some(toLong(data))

  def OptionEmpty(s:String) = if(s.isEmpty()) None else Some(s)

  def parseBlock(data:String):Seq[BLOCK]
  def parseTransaction(data:String):Seq[TRANSACTION]
  def parseTokenTransfer(data:String):Seq[TRANSFER]
  def parseEventLog(data:String):Seq[LOG]
  def parseTx(data:String):Seq[TX]  

}
