package io.syspulse.haas.ingest.eth.uri

/* 
somnia://
*/

object SomniaURI {
  val PREFIX_MAINNET = "somnia"
  val PREFIX_TESTNET = "somnia_test"
  val CHAIN_ID = 50312
  val COIN = "STT"

  val MAINNET_URL = " https://dream-rpc.somnia.network/"
  val TESTNET_URL = "https://rpc.ankr.com/somnia_testnet/6e3fd81558cf77b928b06b38e9409b4677b637118114e83364486294d5ff4811"
}

case class SomniaURI(somUri:String) extends RpcURI(somUri) {
  
  override def uri:String = getBaseUrl()

  override def getMainnet():String = SomniaURI.MAINNET_URL
  override def getTestnet():String = SomniaURI.TESTNET_URL
  override def getMainnetPrefix():String = SomniaURI.PREFIX_MAINNET
  override def getTestnetPrefix():String = SomniaURI.PREFIX_TESTNET
  override def getApiKeyVar():String = "SOMNIA_API_KEY"
  
}