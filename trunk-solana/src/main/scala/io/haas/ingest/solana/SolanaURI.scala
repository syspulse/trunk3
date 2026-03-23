package io.haas.ingest.solana

/* 
sol://host:port/api
*/

object SolanaURI {
  val PREFIX = "sol"
  val PREFIX2 = "solana"
  val PREFIX_DEV = s"${PREFIX}:dev"
  val PREFIX_TEST = s"${PREFIX}:test"
  val PREFIX_PROD = s"${PREFIX}:prod"
  val PREFIX_DEV2 = s"${PREFIX2}:dev"
  val PREFIX_TEST2 = s"${PREFIX2}:test"
  val PREFIX_PROD2 = s"${PREFIX2}:prod"
  val DEFAULT_HOST_DEV = "https://api.devnet.solana.com"
  val DEFAULT_HOST_TEST = "https://api.testnet.solana.com"
  val DEFAULT_HOST_PROD = "https://api.mainnet-beta.solana.com"
}

case class SolanaURI(rpcUri:String,apiSuffix:String="",apiToken:String="") {  
  
  def rpcUrl(apiToken:String = "") = s"${apiToken}"
  
  private var rUri = ""

  def uri:String = rUri

  def parse(rpcUri:String):String = {

    rpcUri.trim.split("://|[/]").toList match {      
      case (SolanaURI.PREFIX_DEV | SolanaURI.PREFIX_DEV2 | "solana:dev") :: Nil => SolanaURI.DEFAULT_HOST_DEV + rpcUrl(apiToken) + apiSuffix
      case (SolanaURI.PREFIX_TEST | SolanaURI.PREFIX_TEST2 | "solana:test") :: Nil => SolanaURI.DEFAULT_HOST_TEST + rpcUrl(apiToken) + apiSuffix
      case (SolanaURI.PREFIX | SolanaURI.PREFIX2 ) :: Nil => SolanaURI.DEFAULT_HOST_PROD + rpcUrl(apiToken) + apiSuffix
      case "http" :: _ => rpcUri
      case "https" :: _ => rpcUri
      case _ => rpcUri
    }
  }

  rUri = parse(rpcUri)    
}