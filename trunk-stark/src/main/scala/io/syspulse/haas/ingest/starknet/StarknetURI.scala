package io.syspulse.haas.ingest.starknet

/* 
stark://host:port/api
*/

case class StarknetURI(rpcUri:String,apiSuffix:String="",apiToken:String="") {
  val PREFIX = "stark://"

  val DEFAULT_HOST = "starknet-mainnet.infura.io"
  def rpcUrl(apiToken:String = "") = s"/v3/${apiToken}"
  
  private var rUri = ""

  def uri:String = rUri

  def parse(rpcUri:String):String = {

    rpcUri.trim.split("://|[/]").toList match {      
      case "stark" :: Nil => "https://" + DEFAULT_HOST + rpcUrl(apiToken) + apiSuffix
      case "http" :: _ => rpcUri
      case "https" :: _ => rpcUri
      case _ => rpcUri
    }
  }

  rUri = parse(rpcUri)    
}