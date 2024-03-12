package io.syspulse.haas.ingest.stellar

/* 
stellar://
stellar:testnet//
stellar:testnet//host:port/api
*/

case class StellarURI(uri0:String,apiSuffix:String="",apiKey:String="") {  

  private val (rUri:String,rNetType:String,ruser:Option[String],rpass:Option[String],rhost:String) = 
    parse(uri0)

  def uri:String = rUri
  def netType:String = rNetType     
  def user:Option[String] = ruser
  def pass:Option[String] = rpass
  def host:String = rhost   

  def parseCred(userPass:String) = userPass.split(":").toList match {
    case u :: p :: _ => (Some(u),Some(p))
    case u :: Nil => (Some(u),None)
    case _ => (None,None)
  }

  def parseNetType(netType:String) = netType.trim.toLowerCase match {
    case "mainnet" => "https://horizon.stellar.org"
    case "testnet" => "https://horizon-testnet.stellar.org"
    case "futurenet" => "https://horizon-futurenet.stellar.org"
    case _ => s"https://horizon-${netType}.stellar.org"
  }

  def parseApiToken(apiKey:String) = if(apiKey.trim.isEmpty()) "" else "/"+apiKey
    
  def parse(uri:String):(String,String,Option[String],Option[String],String) = {
    val (ntype,user,pass,hostUrl) = uri.split("://|[@:]").toList  match {
      
      case "stellar" :: ntype :: user :: pass :: host :: portUrl :: Nil => 
        (ntype,Some(user),Some(pass),host + portUrl)

      case "stellar" :: ntype :: user :: pass :: hostUrl :: Nil => 
        (ntype,Some(user),Some(pass),hostUrl)
      
      case "stellar" :: ntype :: hostPortUrl :: Nil =>
        (ntype,None,None,hostPortUrl)

      case "stellar" :: ntype :: Nil => 
        (ntype,None,None,"")

      case "stellar" :: Nil => 
        ("testnet",None,None,"")

      case _ => 
        ("testnet",None,None,uri)
    }

    val u = if(hostUrl.isEmpty) s"${parseNetType(ntype)}${parseApiToken(apiKey)}" else hostUrl+parseApiToken(apiKey)
    (u,ntype,user,pass,hostUrl)
  }
}