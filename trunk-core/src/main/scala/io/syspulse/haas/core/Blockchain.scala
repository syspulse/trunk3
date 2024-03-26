package io.syspulse.haas.core

import scala.jdk.CollectionConverters._
import io.syspulse.skel.Ingestable

case class Blockchain(
  name:String,
  id:Option[String] = None  // chain_id 
) {
  def asLong:Long = id.getOrElse("0").toLong
}

object Blockchain {
  type ID = String

  val ETHEREUM = Blockchain("ethereum",Some("1"))
  val BSC_MAINNET = Blockchain("bsc",Some("324"))
  val ARBITRUM_MAINNET = Blockchain("arbitrum",Some("42161"))
  val OPTIMISM_MAINNET = Blockchain("optimism",Some("10"))
  val POLYGON_MAINNET = Blockchain("polygon",Some("137"))

  val SCROLL_MAINNET = Blockchain("scroll",Some("534352"))
  val ZKSYNC_MAINNET = Blockchain("zksync",Some("324"))

  val LINEA_MAINNET = Blockchain("linea",Some("59144")) 
  val BASE_MAINNET = Blockchain("base",Some("8453")) 
  
  val SEPOLIA = Blockchain("sepolia",Some("11155111"))
  val ANVIL = Blockchain("anvil",Some("31337"))
}
