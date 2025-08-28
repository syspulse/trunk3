package io.syspulse.haas.reorg

import java.util.concurrent.atomic.AtomicLong

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger
import io.syspulse.haas.reorg.ReorgBlock

class ReorgBlock1(depth:Int = 10,reorgFile:String = "") extends ReorgBlock(depth,reorgFile) {
  
  def range(cursor:Long,lastBlock:Long) = {
    (cursor - depth) to lastBlock
  }
  
}

