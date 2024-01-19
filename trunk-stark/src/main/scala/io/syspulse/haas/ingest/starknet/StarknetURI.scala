package io.syspulse.haas.ingest.starknet

/* 
starknet://host:port/api
*/

case class StarknetURI(rpcUri:String,apiSuffix:String="",apiToken:String="") {
  val PREFIX = "starknet://"

  val DEFAULT_HOST = "starknet-mainnet.infura.io"
  def rpcUrl(apiToken:String = "") = s"/v3/${apiToken}"
  
  private var rUri = ""

  def uri:String = rUri

  def parse(rpcUri:String):String = {
    val prefix = "https://"

    rpcUri.trim.stripPrefix(PREFIX).split("/").toList match {      
      case host :: path :: _ => 
        prefix + host + path
      case "" :: Nil => 
        prefix + DEFAULT_HOST + rpcUrl(apiToken) + apiSuffix
      case host :: Nil => 
        prefix + DEFAULT_HOST + rpcUrl(apiToken) + apiSuffix
      case Nil => 
        prefix + DEFAULT_HOST + rpcUrl(apiToken) + apiSuffix
    }
  }

  rUri = parse(rpcUri)    
}