package io.syspulse.haas.ingest.solana

/* 
sol://host:port/api
*/

case class SolanaURI(rpcUri:String,apiSuffix:String="",apiToken:String="") {  
  val DEFAULT_HOST_DEV = "https://api.devnet.solana.com"
  val DEFAULT_HOST_TEST = "https://api.testnet.solana.com"
  val DEFAULT_HOST_PROD = "https://api.mainnet-beta.solana.com"
  def rpcUrl(apiToken:String = "") = s"${apiToken}"
  
  private var rUri = ""

  def uri:String = rUri

  def parse(rpcUri:String):String = {

    rpcUri.trim.split("://|[/]").toList match {      
      case "sol:dev" :: Nil => DEFAULT_HOST_DEV + rpcUrl(apiToken) + apiSuffix
      case "sol:test" :: Nil => DEFAULT_HOST_TEST + rpcUrl(apiToken) + apiSuffix
      case "sol" :: Nil => DEFAULT_HOST_PROD + rpcUrl(apiToken) + apiSuffix
      case "http" :: _ => rpcUri
      case "https" :: _ => rpcUri
      case _ => rpcUri
    }
  }

  rUri = parse(rpcUri)    
}