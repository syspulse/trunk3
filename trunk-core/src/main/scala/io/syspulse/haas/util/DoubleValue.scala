package io.syspulse.haas.core

import scala.collection.mutable
import scala.jdk.CollectionConverters._
import io.jvm.uuid._
import scala.math.BigInt

object DoubleValue {
  def isZero(d:Double) = Math.abs(d).toLong == 0
  def ==(d1:Double,d2:Double) = isZero(d1-d2)
}
