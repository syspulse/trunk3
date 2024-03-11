package io.syspulse.haas.intercept

import scala.util.{Try,Success,Failure}
import com.typesafe.scalalogging.Logger

import io.syspulse.haas.intercept.script.Script
import io.syspulse.haas.intercept.script._

class Interceptor(scriptSrc:String) {
  protected val log = Logger(this.getClass)
  
  val script = Script(
    "name-1",
    src = scriptSrc
  )

  val engine = ScriptEngine.engines.get(script.typ)

  def scan[E](e:E):Option[String] = {
            
    if(! engine.isDefined) {
      log.error(s"engine not fonud: ${script.typ}")
      return None
    }

    val data = Map[String,Any]("inputs" -> e)
              
    val r = engine.get.run(script.src,data)

    log.info(s"${script}: ${Console.YELLOW}${r}${Console.RESET}")
              
    r match {
      case Failure(e) => 
        log.error(s"script failed: ${e.getMessage()}")
        None
      case Success(null) =>
        // ignore, script is not interested
        None
      case Success(res) =>
        Some(res.toString)
    }

  }
}