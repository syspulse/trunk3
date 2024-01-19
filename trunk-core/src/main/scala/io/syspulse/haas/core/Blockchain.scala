package io.syspulse.haas.core

import scala.jdk.CollectionConverters._
import io.syspulse.skel.Ingestable

case class Blockchain(
  id:Blockchain.ID,
  cid:Option[String] = None  // chain_id 
)

object Blockchain {
  type ID = String

  val ETHEREUM_MAINNET:ID = "ethereum"
  val ZKSYNC_MAINNET:ID = "zksync"
  val OPTIMISM_MAINNET:ID = "optimism"
  val ARBITRUM_MAINNET:ID = "arbitrum"
}