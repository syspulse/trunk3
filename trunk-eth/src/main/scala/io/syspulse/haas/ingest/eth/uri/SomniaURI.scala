package io.syspulse.haas.ingest.eth.uri

/* 
somnia://
*/

object SomniaURI {
  val PREFIX_MAINNET = "somnia"
  val PREFIX_TESTNET = "somnia_test"
  val CHAIN_ID = 50312
  val COIN = "STT"
}

case class SomniaURI(somUri:String) extends RpcURI(somUri) {
  
  override def uri:String = getBaseUrl()

  override def getMainnet():String = "https://data-node.somnia.io/v3"
  override def getTestnet():String = "https://dream-rpc.somnia.network"
  override def getMainnetPrefix():String = SomniaURI.PREFIX_MAINNET
  override def getTestnetPrefix():String = SomniaURI.PREFIX_TESTNET
  override def getApiKeyVar():String = "SOMNIA_API_KEY"
  
}