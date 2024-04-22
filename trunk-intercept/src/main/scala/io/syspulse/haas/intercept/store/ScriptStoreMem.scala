package io.syspulse.haas.intercept.store

import scala.util.Try
import scala.util.{Success,Failure}
import scala.collection.immutable

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.scalalogging.Logger

import io.jvm.uuid._

import io.syspulse.haas.intercept.Script

class ScriptStoreMem extends ScriptStore {
  val log = Logger(s"${this}")
  
  var scripts: Map[String,Script] = Map()

  def all:Seq[Script] = scripts.values.toSeq

  def size:Long = scripts.size

  def +(o:Script):Try[Script] = { 
    scripts = scripts + (o.id -> o)
    log.debug(s"add: ${o}")
    Success(o)
  }

  def del(id:String):Try[String] = { 
    val sz = scripts.size
    scripts = scripts - id;
    log.info(s"del: ${id}")
    if(sz == scripts.size) Failure(new Exception(s"not found: ${id}")) else Success(id)  
  }

  def ?(id:String):Try[Script] = scripts.get(id) match {
    case Some(u) => Success(u)
    case None => Failure(new Exception(s"not found: ${id}"))
  }
    
  def clear():Try[ScriptStore] = {
    scripts = Map()
    Success(this)
  }

  def update(id:String,src:String):Try[Script] = {
    this.?(id) match {
      case Success(o) => 
        val o1 = modify(o,src)
        this.+(o1)
        Success(o1)
      case f => f
    }
  }
}
