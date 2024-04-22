package io.syspulse.haas.intercept.server

import com.typesafe.scalalogging.Logger
import io.jvm.uuid._
import scala.util.{Try,Success,Failure}
import java.nio.file.Paths

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.FileIO

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.headers.`Content-Type`
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.model.StatusCodes._

import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings

import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.parameters.RequestBody
// import javax.ws.rs.{Consumes, POST, GET, DELETE, Path, Produces}
// import javax.ws.rs.core.MediaType
import jakarta.ws.rs.{Consumes, POST, PUT, GET, DELETE, Path, Produces}
import jakarta.ws.rs.core.MediaType


import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter

import io.syspulse.skel.service.Routeable
import io.syspulse.skel.service.CommonRoutes

import io.syspulse.skel.Command

import io.syspulse.skel.auth.permissions.Permissions
import io.syspulse.skel.auth.RouteAuthorizers

import io.syspulse.haas.intercept._
import io.syspulse.haas.intercept.store.InterceptRegistry
import io.syspulse.haas.intercept.store.InterceptRegistryProto
import io.syspulse.haas.intercept.server.InterceptJson
import io.syspulse.skel.service.telemetry.TelemetryRegistry
import io.syspulse.skel.service.ws.WebSocket
import scala.concurrent.ExecutionContext
//import io.syspulse.skel.cron.CronFreq
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit

import io.syspulse.haas.ingest.Config

@Path("/")
class InterceptRoutes(registry: ActorRef[Command])(implicit context: ActorContext[_],config:Config,ex:ExecutionContext) 
  extends WebSocket(config.timeoutIdle)(ex) with CommonRoutes with Routeable with RouteAuthorizers {
  
  override val log = Logger(s"${this}")

  implicit val system: ActorSystem[_] = context.system    
  implicit val permissions = Permissions()


  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import spray.json._  
  import InterceptJson._
  import InterceptRegistryProto._

  // quick flag to trigger Websocket update
  @volatile var updated = false
    
  def getScripts(): Future[Try[Scripts]] = registry.ask(GetScripts)
  def getScript(id: String): Future[Try[Scripts]] = registry.ask(GetScript(id, _))
  
  def createScript(req: ScriptCreateReq): Future[Try[Scripts]] = registry.ask(CreateScript(req, _))
  def updateScript(req: ScriptUpdateReq): Future[Try[Scripts]] = registry.ask(UpdateScript(req, _))
  def deleteScript(id: String): Future[Try[String]] = registry.ask(DeleteScript(id, _))
  

  @GET @Path("/{id}") @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(tags = Array("intercept"),summary = "Return Script by id",
    parameters = Array(new Parameter(name = "id", in = ParameterIn.PATH, description = "Script id (uuid)")),
    responses = Array(new ApiResponse(responseCode="200",description = "Script returned",content=Array(new Content(schema=new Schema(implementation = classOf[Script])))))
  )
  def getScriptRoute(id: String) = get {
    rejectEmptyResponse {
      onSuccess(getScript(id)) { r =>
        complete(r)
      }
    }
  }

  
  @GET @Path("/") @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(tags = Array("intercept"), summary = "Return all Scripts",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "List of Scripts",content = Array(new Content(schema = new Schema(implementation = classOf[Scripts])))))
  )
  def getScriptsRoute() = get {
    complete(getScripts())
  }

  @DELETE @Path("/{id}") @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(tags = Array("intercept"),summary = "Delete Script by id",
    parameters = Array(new Parameter(name = "id", in = ParameterIn.PATH, description = "Script id (uuid)")),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Script deleted",content = Array(new Content(schema = new Schema(implementation = classOf[Script])))))
  )
  def deleteScriptRoute(id: String) = delete {
    onSuccess(deleteScript(id)) { r =>
      updated = true
      complete(StatusCodes.OK, r)
    }
  }

  @POST @Path("/") @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(tags = Array("intercept"),summary = "Create Script",
    requestBody = new RequestBody(content = Array(new Content(schema = new Schema(implementation = classOf[ScriptCreateReq])))),
    responses = Array(new ApiResponse(responseCode = "200", description = "Script",content = Array(new Content(schema = new Schema(implementation = classOf[Script])))))
  )
  def createScriptRoute = post {
    entity(as[ScriptCreateReq]) { req =>
      onSuccess(createScript(req)) { r =>
        // update subscribers
        if(r.isSuccess && config.freq == 0L) 
          broadcastText(r.get.toJson.compactPrint) 
        else
          updated = true
        complete(StatusCodes.Created, r)
      }
    }
  }

  @PUT @Path("/") @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(tags = Array("intercept"),summary = "Update Script",
    requestBody = new RequestBody(content = Array(new Content(schema = new Schema(implementation = classOf[ScriptUpdateReq])))),
    responses = Array(new ApiResponse(responseCode = "200", description = "Script",content = Array(new Content(schema = new Schema(implementation = classOf[Script])))))
  )
  def updateScriptRoute(id:String) = put {
    entity(as[ScriptUpdateReq]) { req =>
      onSuccess(updateScript(req.copy(id = id))) { r =>
        if(r.isSuccess && config.freq == 0L) 
          broadcastText(r.get.toJson.compactPrint)
        else
          updated = true
        complete(StatusCodes.OK, r)
      }
    }
  }

  // can trigger update externally directly
  def update(req:ScriptUpdateReq) = {
    updateScript(req).map{ r => {
      log.debug(s"update: ${r}")
      if(r.isSuccess && config.freq == 0L) 
        broadcastText(r.get.toJson.compactPrint)
      else
        updated = true
    }}
  }

  // run cron
  // new CronFreq(
  //   () => {
  //     log.debug(s"cron: updated=${updated}")
  //     if(updated) {
  //       getScripts().map( _.map{ oo =>
  //         broadcastText(oo.toJson.compactPrint)
  //         updated = false
  //       })
  //     }
  //     // always success
  //     true
  //   },
  //   interval = FiniteDuration(config.freq,TimeUnit.MILLISECONDS),
  //   delay = config.freq
  // ).start()

    
  val corsAllow = CorsSettings(system.classicSystem)
    .withAllowCredentials(true)
    .withAllowedMethods(Seq(HttpMethods.OPTIONS,HttpMethods.GET,HttpMethods.POST,HttpMethods.PUT,HttpMethods.DELETE,HttpMethods.HEAD))

  override def routes: Route = cors(corsAllow) {
      concat(
        pathEndOrSingleSlash {
          concat(
            authenticate()(authn =>
              getScriptsRoute()
              ~
              createScriptRoute  
            ),            
          )
        },
        pathPrefix("ws") { 
          extractClientIP { addr => {
            log.info(s"<-- ws://${addr}")

            authenticate()(authn => {              
              // for experiments only
              extractOfferedWsProtocols { protocols => {
                log.debug(s"Websocket Protocols: ${protocols}")

                pathPrefix(Segment) { group =>
                  handleWebSocketMessages(this.listen(group))
                } ~
                pathEndOrSingleSlash {
                  handleWebSocketMessages(this.listen())
                }
              }}
            })
          }}
        },
        pathPrefix(Segment) { id => 
          pathEndOrSingleSlash {
            authenticate()(authn => {
              // authorize(Permissions.isAdmin(authn) || Permissions.isService(authn)) {}
              updateScriptRoute(id) ~
              getScriptRoute(id) ~
              deleteScriptRoute(id)
            }) 
          }
        },        
      )
  }
}
