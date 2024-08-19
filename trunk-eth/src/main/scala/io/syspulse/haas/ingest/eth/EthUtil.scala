package io.syspulse.haas.ingest.eth

import java.util.concurrent.atomic.AtomicLong
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

import io.syspulse.skel.util.Util
import io.syspulse.haas.ingest.IngestUtil

object EthUtil {
  
  def decodeERC20Transfer(e:etl.LogTx) = {
    if(e.topics.size > 0 && e.topics(0) == "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef") {
      // standard is to have 2 values in topic
      e.topics.size match {
        case 3 => 
          val from = s"0x${e.topics(1).drop(24 + 2)}".toLowerCase()
          val to = s"0x${e.topics(2).drop(24 + 2)}".toLowerCase()
          val v = IngestUtil.toBigInt(e.data)
          Some((from,to,v))
        case 1 =>
          // CrytpKitty style (https://etherscan.io/tx/0x9514ca69668169270225a1d2d713dee6aa3fc797107d1d710d4d9c622bfcc3bb#eventlog)
          val from = s"0x${e.data.drop(2 + 24)}".toLowerCase()
          val to = s"0x${e.data.drop(2 + 32 * 2 + 24 * 2)}".toLowerCase()
          val v = s"0x${e.data.drop(2 + 32 * 2 + 32 * 2 + 24 * 2)}" 
          Some((from,to,v))
      }
    } else 
      None
  }
}
