package io.syspulse.haas.ingest.eth.uri

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.TryValues._

import io.syspulse.skel.util.Util

class EthURISpec extends AnyWordSpec with Matchers {

  "EthURI" should {
    
    "parse 'eth://' to default mainnet URL" in {
      val u = EthURI("eth://")      
      u.uri should === ("https://eth.llamarpc.com")
      u.apiKey should === ("")
      u.max should === (10)
      u.throttle should === (1000L)
      u.timeout.toMillis should === (10000L)
    }

    "parse 'eth://' with API Key in URL" in {
      val u = EthURI("eth://123")      
      u.uri should === ("https://eth.llamarpc.com")
      u.apiKey should === ("123")
      u.max should === (10)
    }

    "parse 'eth://' with options" in {
      val u = EthURI("eth://?max=20&throttle=500&timeout=5000")      
      u.uri should === ("https://eth.llamarpc.com")
      u.apiKey should === ("")
      u.max should === (20)
      u.throttle should === (500L)
      u.timeout.toMillis should === (5000L)
      u.ops should contain ("max" -> "20")
      u.ops should contain ("throttle" -> "500")
      u.ops should contain ("timeout" -> "5000")
    }

    "parse 'eth://' with API Key and options" in {
      val u = EthURI("eth://123?max=15&throttle=2000")      
      u.uri should === ("https://eth.llamarpc.com")
      u.apiKey should === ("123")
      u.max should === (15)
      u.throttle should === (2000L)
      u.ops should contain ("max" -> "15")
      u.ops should contain ("throttle" -> "2000")
    }

    "parse 'sepolia://' to testnet URL" in {
      val u = EthURI("sepolia://")      
      u.uri should === ("https://ethereum-sepolia-rpc.publicnode.com")
      u.apiKey should === ("")
      u.max should === (10)
    }

    "parse 'sepolia://' with API Key" in {
      val u = EthURI("sepolia://456")      
      u.uri should === ("https://ethereum-sepolia-rpc.publicnode.com")
      u.apiKey should === ("456")
    }

    "parse 'http://geth:8545'" in {
      val u = EthURI("http://geth:8545")      
      u.uri should === ("http://geth:8545")
      u.apiKey should === ("")
      u.max should === (10)
    }

    "parse 'https://geth:8545/api/'" in {
      val u = EthURI("https://geth:8545/api/")      
      u.uri should === ("https://geth:8545/api/")
      u.apiKey should === ("")
      u.max should === (10)
    }

    "parse 'http://geth:8545' with options" in {
      val u = EthURI("http://geth:8545?max=25&throttle=1000")      
      u.uri should === ("http://geth:8545")
      u.apiKey should === ("")
      u.max should === (25)
      u.throttle should === (1000L)
      u.ops should contain ("max" -> "25")
      u.ops should contain ("throttle" -> "1000")
    }

    "parse 'https://geth:8545/api/' with API Key in options" in {
      val u = EthURI("https://geth:8545/api/?apiKey=789&max=30")      
      u.uri should === ("https://geth:8545/api/")
      u.apiKey should === ("789")
      u.max should === (30)
      u.ops should contain ("apiKey" -> "789")
      u.ops should contain ("max" -> "30")
    }

    "parse 'ws://geth:8546'" in {
      val u = EthURI("ws://geth:8546")      
      u.uri should === ("ws://geth:8546")
      u.apiKey should === ("")
      u.max should === (10)
    }

    "parse 'wss://geth:8546'" in {
      val u = EthURI("wss://geth:8546")      
      u.uri should === ("wss://geth:8546")
      u.apiKey should === ("")
      u.max should === (10)
    }

    "parse 'somnia://' to Somnia mainnet" in {
      val u = EthURI("somnia://")      
      u.uri should === (SomniaURI.MAINNET_URL)
      u.apiKey should === ("")
      u.max should === (10)
    }

    "parse 'somnia_test://' to Somnia testnet" in {
      val u = EthURI("somnia_test://")      
      u.uri should === (SomniaURI.TESTNET_URL)
      u.apiKey should === ("")
      u.max should === (10)
    }

    "parse 'somnia://' with API Key" in {
      val u = EthURI("somnia://abc123")      
      // Current implementation has a bug: returns raw URI and empty API key
      u.uri should === (SomniaURI.MAINNET_URL)
      u.apiKey should === ("abc123")
      u.max should === (10)
    }

    "parse 'somnia://' with options" in {
      val u = EthURI("somnia://?max=40&throttle=3000")      
      // Current implementation has a bug: returns raw URI without options
      u.uri should === (SomniaURI.MAINNET_URL)
      u.max should === (40)
      u.throttle should === (3000L)
    }

    "handle complex URL with multiple options" in {
      val u = EthURI("eth://api123?max=50&throttle=1500&timeout=8000&custom=value")      
      u.uri should === ("https://eth.llamarpc.com")
      u.apiKey should === ("api123")
      u.max should === (50)
      u.throttle should === (1500L)
      u.timeout.toMillis should === (8000L)
      u.ops should contain ("max" -> "50")
      u.ops should contain ("throttle" -> "1500")
      u.ops should contain ("timeout" -> "8000")
      u.ops should contain ("custom" -> "value")
    }

    "handle URL with empty options" in {
      val u = EthURI("eth://?")      
      u.uri should === ("https://eth.llamarpc.com")
      u.apiKey should === ("")
      u.max should === (10)
      u.ops should be (empty)
    }

    "handle URL with malformed options gracefully" in {
      val u = EthURI("eth://?invalid&max=30&=value&key=")      
      u.uri should === ("https://eth.llamarpc.com")
      u.apiKey should === ("")
      u.max should === (30)
      u.ops should contain ("max" -> "30")
      // Malformed options should be ignored or handled as empty keys
      u.ops should not contain key("invalid")
    }

    "handle edge case with empty string" in {
      val u = EthURI("")      
      u.uri should === ("")
      u.apiKey should === ("")
      u.max should === (10)
    }

    "handle unsupported feed URI" in {
      val u1 = EthURI("file://1")
      u1.uri should === ("")
      u1.apiKey should === ("")      

      val u2 = EthURI("file://")
      u2.uri should === ("")
      u2.apiKey should === ("")

      val u3 = EthURI("123")
      u3.uri should === ("")
      u3.apiKey should === ("")
    }

    "handle edge case with just protocol" in {
      val u = EthURI("eth://")      
      u.uri should === ("https://eth.llamarpc.com")
      u.apiKey should === ("")
      u.max should === (10)
    }

    "handle URL with special characters in options" in {
      val u = EthURI("eth://?special=value%20with%20spaces&encoded=test%2Fpath")      
      u.uri should === ("https://eth.llamarpc.com")
      u.apiKey should === ("")
      u.ops should contain ("special" -> "value%20with%20spaces")
      u.ops should contain ("encoded" -> "test%2Fpath")
    }

    "handle URL with numeric values in options" in {
      val u = EthURI("eth://?max=100&throttle=5000&timeout=30000")      
      u.uri should === ("https://eth.llamarpc.com")
      u.max should === (100)
      u.throttle should === (5000L)
      u.timeout.toMillis should === (30000L)
    }

    "handle URL with boolean-like values in options" in {
      val u = EthURI("eth://?debug=true&verbose=false&enabled=1")      
      u.uri should === ("https://eth.llamarpc.com")
      u.ops should contain ("debug" -> "true")
      u.ops should contain ("verbose" -> "false")
      u.ops should contain ("enabled" -> "1")
    }

    "handle URL with multiple same keys (last wins)" in {
      val u = EthURI("eth://?max=10&max=20&max=30")      
      u.uri should === ("https://eth.llamarpc.com")
      u.max should === (30)
      u.ops should contain ("max" -> "30")
    }

    "handle URL with mixed case in options" in {
      val u = EthURI("eth://?Max=25&Throttle=1500&TIMEOUT=8000")      
      u.uri should === ("https://eth.llamarpc.com")
      u.max should === (10) // Default value since "Max" != "max"
      u.throttle should === (1000L) // Default value since "Throttle" != "throttle"
      u.timeout.toMillis should === (10000L) // Default value since "TIMEOUT" != "timeout"
      u.ops should contain ("Max" -> "25")
      u.ops should contain ("Throttle" -> "1500")
      u.ops should contain ("TIMEOUT" -> "8000")
    }

    "handle URL with path segments" in {
      val u = EthURI("eth://api123/path/to/endpoint?max=15")      
      u.uri should === ("https://eth.llamarpc.com")
      u.apiKey should === ("api123")
      u.max should === (15)
      u.ops should contain ("max" -> "15")
    }

    "handle URL with port numbers" in {
      val u = EthURI("eth://api123:8080?max=20")      
      u.uri should === ("https://eth.llamarpc.com")
      u.apiKey should === ("api123")
      u.max should === (20)
    }

    "handle URL with hostnames" in {
      val u = EthURI("eth://api123@hostname.com?max=25")      
      u.uri should === ("https://eth.llamarpc.com")
      u.apiKey should === ("api123")
      u.max should === (25)
    }

    "handle URL with complex path and options" in {
      val u = EthURI("eth://api123@hostname.com:8080/path/to/endpoint?max=30&throttle=2000")      
      u.uri should === ("https://eth.llamarpc.com")
      u.apiKey should === ("api123")
      u.max should === (30)
      u.throttle should === (2000L)
    }

    "handle testnet URL with API key and options" in {
      val u = EthURI("sepolia://testkey?max=40&throttle=3000")      
      u.uri should === ("https://ethereum-sepolia-rpc.publicnode.com")
      u.apiKey should === ("testkey")
      u.max should === (40)
      u.throttle should === (3000L)
    }

    "handle Somnia URL with API key and options" in {
      val u = EthURI("somnia://somnia_key?max=50&throttle=4000")      
      // Current implementation has a bug: returns raw URI without options
      u.uri should === (SomniaURI.MAINNET_URL)
      u.apiKey should === ("somnia_key")
      u.max should === (50)
      u.throttle should === (4000L)
    }

    "handle Somnia testnet URL with API key and options" in {
      val u = EthURI("somnia_test://test_somnia_key?max=60&throttle=5000")      
      // Current implementation has a bug: returns raw URI without options
      u.uri should === (SomniaURI.TESTNET_URL)
      u.apiKey should === ("test_somnia_key")
      u.max should === (60)
      u.throttle should === (5000L)
    }
  }
}