package io.syspulse.haas.ingest.eth

import java.util.concurrent.atomic.AtomicLong
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

import io.syspulse.skel.util.Util

object EthUtil {
  
  def decodeERC20Transfer(e:etl.LogTx) = io.syspulse.skel.blockchain.eth.EthUtil.decodeERC20Transfer(e.data,e.topics)    
}
