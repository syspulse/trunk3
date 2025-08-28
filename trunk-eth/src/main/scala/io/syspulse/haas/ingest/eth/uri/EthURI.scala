package io.syspulse.haas.ingest.eth.uri

import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import io.syspulse.skel.util.Util

/* 
eth://
http://geth:8545
https://mainnet.infura.io
*/

abstract class RpcURI(uri0:String) {
  def uri:String = getBaseUrl()

  def getMainnet():String
  def getTestnet():String
  def getMainnetPrefix():String
  def getTestnetPrefix():String
  def getApiKeyVar():String

  private val (_apiKey:String,_max:Int,_ops:Map[String,String],prefix:String) = parse(uri0)
  
  def apiKey:String = _apiKey
  def max:Int = _max
  // def latest:Int = _ops.get("latest").map(_.toInt).getOrElse(1)
  def ops:Map[String,String] = _ops
  def throttle:Long = _ops.get("throttle").map(_.toLong).getOrElse(1000L)
  def timeout:FiniteDuration = _ops.get("timeout").map(_.toLong).map(FiniteDuration(_,TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(10000L,TimeUnit.MILLISECONDS))
  
  def getBaseUrl():String = prefix match {
    case "" => ""
    case prefix if prefix == getMainnetPrefix() => getMainnet()
    case prefix if prefix == getTestnetPrefix() => getTestnet()    
    case "" => getMainnet()
    case _ => prefix
  }
  
  def parse(uri:String):(String,Int,Map[String,String],String) = {
    
    var (prefix,url1) = uri.split("://").toList match {
      // case getMainnetPrefix() :: url =>  (getMainnetPrefix(),url.headOption.getOrElse(""))
      // case getTestnetPrefix() :: url => (getTestnetPrefix(),url.headOption.getOrElse(""))
      case prefix :: url if prefix == getMainnetPrefix() =>  (getMainnetPrefix(),url.headOption.getOrElse(""))
      case prefix :: url if prefix == getTestnetPrefix() =>  (getTestnetPrefix(),url.headOption.getOrElse(""))
      case prefix :: Nil if prefix == getMainnetPrefix() =>  (getMainnetPrefix(),"")
      case prefix :: Nil if prefix == getTestnetPrefix() =>  (getTestnetPrefix(),"")      
      case ("http" | "https" | "ws" | "wss") :: _ => ("", uri)      
      case _ => ("","")
    }
    
    // resolve options
    val (url:String,ops:Map[String,String]) = url1.split("[\\?&]").toList match {
      case url :: Nil => (url,Map())
      case url :: ops => 
        
        val vars = ops.flatMap(_.split("=").toList match {
          case k :: v :: Nil => Some(k -> v)
          case _ => None
        }).toMap
        
        (url,vars)
      case _ => 
        ("",Map())
    }
    
    val (apiKey, max, ops1) = url.split("[:/@]").toList match {
      case _ if prefix == "" => 
        // speical case for http:// style uri (apiKey is not supported in URL, only as ops
        prefix = url
        (
          ops.get("apiKey").getOrElse(sys.env.get(getApiKeyVar()).getOrElse("")),
          ops.get("max").map(_.toInt).getOrElse(10),
          ops
        )
      case apiKey :: _ =>
        (
          Util.replaceEnvVar(apiKey),
          ops.get("max").map(_.toInt).getOrElse(10),
          ops
        )
      case _ =>
        (
          sys.env.get(getApiKeyVar()).getOrElse(""),
          ops.get("max").map(_.toInt).getOrElse(10),
          ops
        )
    }

    (apiKey,max,ops1,prefix)
  }
}

case class EvmURI(ethUri:String,apiSuffix:String="",apiToken:String="") extends RpcURI(ethUri) {

  override def getMainnet():String = "https://eth.llamarpc.com"
  override def getTestnet():String = "https://ethereum-sepolia-rpc.publicnode.com"
  override def getMainnetPrefix():String = "eth"
  override def getTestnetPrefix():String = "sepolia"
  override def getApiKeyVar():String = "ETH_API_KEY"

  //def ethUrl(apiToken:String = "") = s"v3/${apiToken}"
  
}

object EthURI {
  def apply(uri:String,apiSuffix:String="",apiToken:String=""):RpcURI = {
    
    uri.split("://").toList match {
      case "eth" :: Nil => EvmURI(uri,apiSuffix,apiToken)
      case "sepolia" :: Nil => EvmURI(uri,apiSuffix,apiToken)
      case "holesky" :: Nil => EvmURI(uri,apiSuffix,apiToken)

      case "http" :: _ => EvmURI(uri,apiSuffix,apiToken)
      case "https" :: _ => EvmURI(uri,apiSuffix,apiToken)
      case ("ws" | "wss" ) :: _ => EvmURI(uri,apiSuffix,apiToken)

      case SomniaURI.PREFIX_MAINNET :: _ => SomniaURI(uri)
      case SomniaURI.PREFIX_TESTNET :: _ => SomniaURI(uri)

      case _ => EvmURI(uri,apiSuffix,apiToken)
    }
  }
}