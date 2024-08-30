package io.syspulse.haas.ingest.starknet

/* 
stark://host:port/api
*/

case class StarknetURI(rpcUri:String,apiSuffix:String="",apiToken:String="") {
  
  override def toString = s"${this.getClass.getSimpleName}(${rpcUri},${apiSuffix},${apiToken},${uri})"

  val DEFAULT_HOST = "https://rpc.starknet.lava.build:443"
  val INFURA_HOST = "https://starknet-mainnet.infura.io"

  def rpcUrl(apiToken:String = "") = s"/v3/${apiToken}"
  
  private var rUri = ""

  def uri:String = rUri

  def parse(rpcUri:String):String = {

    rpcUri.trim.split("://|[/]").toList match { 
      case ("stark" | "starknet") :: Nil => DEFAULT_HOST
      case ("stark" | "starknet") :: "infura" :: Nil => INFURA_HOST + rpcUrl(apiToken) + apiSuffix
      case "http" :: _ => rpcUri
      case "https" :: _ => rpcUri
      case _ => rpcUri
    }
  }

  rUri = parse(rpcUri)    
}