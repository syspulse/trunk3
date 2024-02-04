package io.syspulse.haas.ingest.stellar

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
//import org.scalatest.TryValues._

import io.syspulse.skel.util.Util

class StellarURISpec extends AnyWordSpec with Matchers {
  //val testDir = this.getClass.getClassLoader.getResource(".").getPath + "../../../"

  "StellarURI" should {
    
    "parse 'stellar://'" in {
      val u = new StellarURI("stellar://")      
      u.uri should === ("https://horizon-testnet.stellar.org")
    }

    "parse 'stellar:testnet://'" in {
      val u = new StellarURI("stellar:testnet://")      
      u.uri should === ("https://horizon-testnet.stellar.org")
    }

    "parse 'stellar:mainnet://'" in {
      val u = new StellarURI("stellar:mainnet://")      
      u.uri should === ("https://horizon.stellar.org")
    }

    "parse 'stellar:futurenet://'" in {
      val u = new StellarURI("stellar:futurenet://")      
      u.uri should === ("https://horizon-futurenet.stellar.org")
    }

    "parse 'stellar:NET://'" in {
      val u = new StellarURI("stellar:NET://")      
      u.uri should === ("https://horizon-NET.stellar.org")
    }

    "parse 'stellar://' with API Key" in {
      val u = new StellarURI("stellar://",apiKey="123")      
      u.uri should === ("https://horizon-testnet.stellar.org/123")
    }

    "parse 'http://host:8545'" in {
      val u = new StellarURI("http://host:8545")      
      u.uri should === ("http://host:8545")
    }

    "parse 'https://host:8545/api/'" in {
      val u = new StellarURI("https://host:8545/api/")      
      u.uri should === ("https://host:8545/api/")
    }
    
  }
  
}