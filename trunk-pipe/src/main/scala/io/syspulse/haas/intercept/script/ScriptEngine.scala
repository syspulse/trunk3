package io.syspulse.haas.intercept.script

import scala.util.Random
import io.jvm.uuid._
import scala.util.{Success,Failure,Try}
import com.typesafe.scalalogging.Logger

import io.syspulse.skel.dsl.JS

abstract class ScriptEngine(id:ScriptEngine.ID,name:String) {
  protected val log = Logger(s"${this.getClass()}")

  def run(src:String,data:Map[String,Any]):Try[Any]
}

class ScriptJS(id:ScriptEngine.ID, name:String = "") extends ScriptEngine(id,name) {  
  val js = new JS()
  
  override def run(src:String,data:Map[String,Any]):Try[Any] = {    
    try {
      Success(js.run(src,data))
    } catch {
      case e:Throwable => Failure(e)
    }
  }
}


object ScriptEngine {
  val log = Logger(s"${this.getClass()}")

  type ID = String

  val engines = Map(
    "js" -> new ScriptJS("js","javascript")
  )

  log.info(s"Script Engines: ${engines}")

}
