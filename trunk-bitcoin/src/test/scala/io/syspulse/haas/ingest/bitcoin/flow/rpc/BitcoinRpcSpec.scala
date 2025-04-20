package io.syspulse.haas.ingest.bitcoin.flow.rpc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source
import java.io.File

import spray.json._
import io.syspulse.haas.ingest.bitcoin.flow.rpc.RpcJsonProtocol._
import io.syspulse.haas.ingest.bitcoin.BitcoinJson._

class BitcoinRpcSpec extends AnyFlatSpec with Matchers {
  
  "BitcoinRpc" should "deserialize block JSON" in {
    // Read JSON file
    val jsonStr = Source.fromFile("trunk-bitcoin/00000000000000000001125bf96de7b188365ed5b1fcfc8ebc23cf8499c1ca2e.json").mkString
    val json = jsonStr.parseJson.asJsObject
    
    // The actual block data is under "result" field
    val block = json.fields("result").convertTo[RpcBlock]
    
    // Verify some basic block properties
    block.hash shouldBe "00000000000000000001125bf96de7b188365ed5b1fcfc8ebc23cf8499c1ca2e"
    block.height shouldBe 888858
    block.version shouldBe 595042304
    block.versionHex shouldBe "2377a000"
    
    // Verify first transaction (coinbase)
    val coinbaseTx = block.tx.head
    coinbaseTx.txid shouldBe "4e7ad34132938405f6c5050fbc24e7c94205c672938c890cd47ff6062ddbef2e"
    coinbaseTx.hash shouldBe "b22ec70a612512f93adc13590224dd64b38b1f930d4479a9b5d63f21019abc74"
    
    // Verify coinbase input
    coinbaseTx.vin.head.coinbase should not be empty
    coinbaseTx.vin.head.txid shouldBe None  // Coinbase has no txid
    
    // Verify outputs
    coinbaseTx.vout.size shouldBe 5
    coinbaseTx.vout.head.value shouldBe BigDecimal("0")
    
    // Verify total number of transactions
    block.tx.size shouldBe block.nTx
  }
} 