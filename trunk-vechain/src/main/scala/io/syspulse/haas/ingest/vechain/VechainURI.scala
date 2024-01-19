package io.syspulse.haas.ingest.vechain

/* 
vechain://host:port/api
*/

case class VechainURI(rpcUri:String,apiSuffix:String="",apiToken:String="") {
  val PREFIX = "vechain://"

  val DEFAULT_HOST = "mainnetc2.vechain.network"
  def rpcUrl(apiToken:String = "") = ""
  
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