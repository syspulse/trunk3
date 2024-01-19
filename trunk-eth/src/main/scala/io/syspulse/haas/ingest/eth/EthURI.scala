package io.syspulse.haas.ingest.eth

/* 
eth://host:port/api
*/

case class EthURI(ethUri:String,apiSuffix:String="",apiToken:String="") {
  val PREFIX = "eth://"

  val ethHost = "mainnet.infura.io"
  def ethUrl(apiToken:String = "") = s"v3/${apiToken}"

  def uri:String = {
    val prefix = "https://"

    ethUri.trim.stripPrefix(PREFIX).split("[/]").toList match {
      case host :: path :: _ => prefix + host + path
      case "" :: Nil => prefix + ethHost + "/" + ethUrl(apiToken) + apiSuffix
      case host :: Nil => prefix + host + "/" + ethUrl(apiToken) + apiSuffix
      case Nil => prefix + ethHost + "/" + ethUrl(apiToken) + apiSuffix
    }
  }
}