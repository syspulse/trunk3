package io.syspulse.haas.intercept

import scala.util.{Try,Success,Failure}
import com.typesafe.scalalogging.Logger

import scala.jdk.CollectionConverters._
import jdk.nashorn.api.scripting.JSObject

import io.syspulse.haas.intercept.script.Script
import io.syspulse.haas.intercept.script._

case class InterceptResult(txHash:String,data:Map[String,String])

class ScriptInterceptor(scriptSrc:String) {
  protected val log = Logger(this.getClass)
  
  val script = Script(
    "name-1",
    src = scriptSrc
  )

  val engine = ScriptEngine.engines.get(script.typ)

  def scan[E](e:E):Option[InterceptResult] = {

    if(! engine.isDefined) {
      log.error(s"engine not fonud: ${script.typ}")
      return None
    }

    val data = Map[String,Any]("inputs" -> e)
              
    val r = engine.get.run(script.src,data)

    log.debug(s"${script}: ${r}")
              
    r match {
      case Failure(e) => 
        log.error(s"script failed: ${e.getMessage()}")
        None
      case Success(null) =>
        // ignore, script is not interested
        None
      case Success(res) =>
        val jmap = res
          .asInstanceOf[java.util.Map[String,Any]]
          .asScala
          .map{case(k,v)=> k -> {if(v!=null) v.toString else ""}}
          .toMap
        val txHash = jmap.get("tx_hash").getOrElse("").toString
        
        log.info(s"${Console.YELLOW}${jmap}${Console.RESET}")
        
        Some(InterceptResult(txHash,jmap))
    }

  }
}