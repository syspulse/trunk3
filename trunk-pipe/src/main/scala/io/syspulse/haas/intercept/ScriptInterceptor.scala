package io.syspulse.haas.intercept

import scala.util.{Try,Success,Failure}
import com.typesafe.scalalogging.Logger

import scala.jdk.CollectionConverters._
import jdk.nashorn.api.scripting.JSObject

import io.syspulse.haas.intercept.script._

case class InterceptResult(
  sid:String,                   // script Id
  txHash:String,
  data:Map[String,String]
)

class ScriptInterceptor(scripts:Seq[Script]) {
  protected val log = Logger(this.getClass)
    
  val engines:Map[String,ScriptEngine] = Map(
    "js" -> ScriptEngine.engines.get("js").get
  )

  def scan[E](e:E):Seq[InterceptResult] = {
    
    scripts.flatMap( script => {
      val engine = engines.get(script.typ)

      if(engine.isDefined) {
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
            
            Some(InterceptResult(sid = script.id,txHash,jmap))
        }
      } else None

    })
    
  }
}