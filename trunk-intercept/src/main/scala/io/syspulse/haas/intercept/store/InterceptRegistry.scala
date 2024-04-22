package io.syspulse.haas.intercept.store

import scala.util.{Try,Success,Failure}

import scala.collection.immutable
import com.typesafe.scalalogging.Logger
import io.jvm.uuid._

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import io.syspulse.skel.Command

import io.syspulse.haas.intercept._
import io.syspulse.haas.intercept.server._
import io.syspulse.haas.intercept.Script

object InterceptRegistryProto {
  final case class GetScripts(replyTo: ActorRef[Try[Scripts]]) extends Command
  final case class GetScript(id:String,replyTo: ActorRef[Try[Scripts]]) extends Command
  
  final case class CreateScript(req: ScriptCreateReq, replyTo: ActorRef[Try[Scripts]]) extends Command
  final case class UpdateScript(req: ScriptUpdateReq, replyTo: ActorRef[Try[Scripts]]) extends Command  
  final case class DeleteScript(id: String, replyTo: ActorRef[Try[String]]) extends Command
}

object InterceptRegistry {  
  val log = Logger(s"${this}")
  
  import InterceptRegistryProto._
  
  // this var reference is unfortunately needed for Metrics access
  var store: ScriptStore = null 

  def apply(store: ScriptStore = new ScriptStoreMem): Behavior[Command] = {
    this.store = store
    registry(store)
  }

  private def registry(store: ScriptStore): Behavior[Command] = {    
    this.store = store
    
    Behaviors.receiveMessage {
      case GetScripts(replyTo) =>
        try {
          val oo = store.all
          replyTo ! Success(Scripts(oo,total=Some(oo.size)))
        } catch {
          case e:Exception => 
            log.error("failed to get all",e)
            replyTo ! Failure(e)
        }
        Behaviors.same

      case GetScript(id, replyTo) =>
        try {
          val oo = store.??(Seq(id))
          replyTo ! Success( Scripts(oo,total=Some(oo.size)) )
        } catch {
          case e:Exception =>
            log.error(s"failed to get: ${id}",e)
            replyTo ! Failure(e)
        }
        Behaviors.same      

      case CreateScript(req, replyTo) =>
        val store1 = 
          store.?(req.id) match {
            case Success(_) => 
              replyTo ! Failure(new Exception(s"already exists: ${req.id}"))
              Success(store)
            case _ =>  
              val o = Script(req.id, src = req.src)
              val store1 = store.+(o)
              replyTo ! store1.map(_ => Scripts(Seq(o),total=Some(1))) 
          }

        Behaviors.same

      case UpdateScript(req, replyTo) =>        
        // ATTENTION: Update is ++ !
        val o = store.update(req.id,req.src)
        val r = o match {
          case Success(o) => Success(Scripts(Seq(o),total=Some(1)))
          case Failure(e) => 
            // try to create
            val o = Script(req.id, src = req.src)
            store.+(o).map(o => Scripts(Seq(o),total=Some(1)))
        }
        replyTo ! r

        Behaviors.same
      
      case DeleteScript(id, replyTo) =>
        val r = store.del(id)
        r match {
          case Success(o) => replyTo ! Success(id)
          case Failure(e) => replyTo ! Failure(e)
        }
        Behaviors.same
      
    }
  }
}
