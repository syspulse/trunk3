package io.syspulse.haas.ingest.bitcoin

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class BitcoinURISpec extends AnyWordSpec with Matchers {
  "BitcoinURI" should {
    "parse URI with 'bitcoin://' prefix" in {
      val uri = BitcoinURI("bitcoin://")
      uri.url should be(BitcoinURI.DEFAULT_HOST)
      uri.ops should be(empty)
      uri.apiKey should be(None)
      uri.refresh should be("1h")
      uri.timeout should be(30000)
    }

    "parse URI without options" in {
      val uri = BitcoinURI("bitcoin://localhost:8332")
      uri.url should be(BitcoinURI.DEFAULT_HOST)
      uri.ops should be(empty)
      uri.apiKey should be(None)
      uri.refresh should be("1h")
      uri.timeout should be(30000)
    }

    "parse URI with options" in {
      val uri = BitcoinURI("bitcoin://localhost:8332?apiKey=123&refresh=30m&timeout=60000")
      uri.url should be(BitcoinURI.DEFAULT_HOST)
      uri.ops should contain allOf(
        "apiKey" -> "123",
        "refresh" -> "30m",
        "timeout" -> "60000"
      )
      uri.apiKey should be(Some("123"))
      uri.refresh should be("30m")
      uri.timeout should be(60000)
    }

    "parse URI with default host" in {
      val uri = BitcoinURI("bitcoin://")
      uri.url should be("https://rpc.bitcoin.lava.build:443")
      uri.ops should be(empty)
    }

    "parse URI with HTTP protocol" in {
      val uri = BitcoinURI("http://mynode:8332?apiKey=123")
      uri.url should be("http://mynode:8332")
      uri.apiKey should be(Some("123"))
    }

    "parse URI with HTTPS protocol" in {
      val uri = BitcoinURI("https://mynode:8332?apiKey=123")
      uri.url should be("https://mynode:8332")
      uri.apiKey should be(Some("123"))
    }

    "parse URI with multiple options" in {
      val uri = BitcoinURI("bitcoin://localhost:8332?apiKey=123&refresh=30m&timeout=60000&extraParam=value")
      uri.ops should have size 4
      uri.ops should contain allOf(
        "apiKey" -> "123",
        "refresh" -> "30m",
        "timeout" -> "60000",
        "extraParam" -> "value"
      )
    }

    "parse URI with empty option values" in {
      val uri = BitcoinURI("bitcoin://localhost:8332?param1&param2=")
      uri.ops should contain allOf(
        "param1" -> "",
        "param2" -> ""
      )
    }

    "handle malformed URIs" in {
      val uri = BitcoinURI("invalid://uri")
      uri.url should be("https://rpc.bitcoin.lava.build:443")
      uri.ops should be(empty)
    }

    "handle empty URI" in {
      val uri = BitcoinURI("")
      uri.url should be("https://rpc.bitcoin.lava.build:443")
      uri.ops should be(empty)
    }
  }
} 