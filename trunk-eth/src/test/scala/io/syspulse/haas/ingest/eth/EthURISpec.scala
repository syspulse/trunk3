package io.syspulse.haas.ingest.eth

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
//import org.scalatest.TryValues._

import io.syspulse.skel.util.Util

class EthURISpec extends AnyWordSpec with Matchers {
  //val testDir = this.getClass.getClassLoader.getResource(".").getPath + "../../../"

  "EthURI" should {
    
    "parse 'eth://'" in {
      val u = new EthURI("eth://")      
      u.uri should === ("https://mainnet.infura.io/v3/")
    }

    "parse 'eth://' with API Key" in {
      val u = new EthURI("eth://","123")      
      u.uri should === ("https://mainnet.infura.io/v3/123")
    }

    "parse 'http://geth:8545'" in {
      val u = new EthURI("http://geth:8545")      
      u.uri should === ("http://geth:8545")
    }

    "parse 'https://geth:8545/api/'" in {
      val u = new EthURI("https://geth:8545/api/")      
      u.uri should === ("https://geth:8545/api/")
    }
    
  }
  
}