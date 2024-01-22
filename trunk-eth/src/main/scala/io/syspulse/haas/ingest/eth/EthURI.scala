package io.syspulse.haas.ingest.eth

/* 
eth://
http://geth:8545
https://mainnet.infura.io
*/

case class EthURI(ethUri:String,apiSuffix:String="",apiToken:String="") {
  
  val default = "https://mainnet.infura.io"
  def ethUrl(apiToken:String = "") = s"v3/${apiToken}"

  def uri:String = {
  
    ethUri.trim.split("://|[/]").toList match {
      case "eth" :: Nil => default + "/" + ethUrl(apiToken) + apiSuffix
      case "http" :: _ => ethUri
      case "https" :: _ => ethUri
      case _ => ethUri
    }
  }
}