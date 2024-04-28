package io.syspulse.haas.intercept.store

import scala.util.Try

import scala.collection.immutable

import io.jvm.uuid._

import io.syspulse.skel.store.Store
import io.syspulse.haas.intercept.Script

trait ScriptStore extends Store[Script,String] {
  
  def getKey(e: Script): String = e.id
  def +(s:Script):Try[Script]
  
  def del(id:String):Try[String]
  def ?(id:String):Try[Script]  
  def all:Seq[Script]
  def size:Long
  def update(id:String, src:String):Try[Script]
  def clear():Try[ScriptStore]

  protected def modify(o:Script,src:String):Script = {    
    (for {
      o1 <- Some(o.copy(ts = System.currentTimeMillis,src = src,ts0 = o.ts0))
    } yield o1).get    
  }
  
}

