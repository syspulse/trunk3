package io.syspulse.haas.ingest.bitcoin

/* 
bitcoin://host:port/api
*/

object BitcoinURI {
  val PREFIX = "bitcoin://"
  val DEFAULT_HOST = "https://bitcoin-mainnet.public.blastapi.io"
}

case class BitcoinURI(uri:String) {
 
  private val (_url:String,_ops:Map[String,String]) = parse(uri)

  def url:String = _url
  def ops:Map[String,String] = _ops

  def apiKey:Option[String] = _ops.get("apiKey")
  def refresh:String = _ops.get("refresh").getOrElse("1h")
  def timeout:Long = _ops.get("timeout").map(_.toLong).getOrElse(30000)
  def username:String = _ops.get("user").orElse(sys.env.get("BITCOIN_USER")).getOrElse("")
  def password:String = _ops.get("pass").orElse(sys.env.get("BITCOIN_PASS")).getOrElse("")
  
  def parse(uri:String):(String,Map[String,String]) = {
    // resolve options
    val (url:String,ops:Map[String,String]) = uri.split("[\\?&]").toList match {
      case url :: Nil => (url,Map())
      case url :: ops => 
        
        val vars = ops.flatMap(_.split("=").toList match {
          case k :: Nil => Some(k -> "")
          case k :: v :: Nil => Some(k -> v)
          case _ => None
        }).toMap
        
        (url,vars)
      case _ => 
        ("",Map())
    }

    val urlStripped = url.trim.split("://").toList match { 
      case BitcoinURI.PREFIX :: Nil => BitcoinURI.DEFAULT_HOST
      case "http" :: _ => url
      case "https" :: _ => url
      case _ => BitcoinURI.DEFAULT_HOST
    }
      
    val urlFinal = urlStripped
    (urlFinal,ops)
  }
}