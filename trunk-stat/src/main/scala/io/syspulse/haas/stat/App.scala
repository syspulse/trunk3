package io.syspulse.haas.stat

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import java.util.concurrent.TimeUnit
import scala.concurrent.Awaitable
import scala.concurrent.{Await, ExecutionContext, Future}
import io.syspulse.skel.FutureAwaitable._
import java.util.concurrent.Executors

import com.typesafe.scalalogging.Logger
import io.jvm.uuid._

import io.syspulse.skel
import io.syspulse.skel.ingest.flow.Pipeline
import io.syspulse.skel.util.Util
import io.syspulse.skel.config._

import io.syspulse.haas.stat.flow._
import io.syspulse.haas.ingest.eth

import io.syspulse.skel.odometer._
import io.syspulse.skel.odometer.store._
import io.syspulse.skel.odometer.server.OdoRoutes

case class Config(  
  host:String="0.0.0.0",
  port:Int=8080,
  uri:String = "/api/v1/stat",
  
  feed:Seq[String] = Seq(""),
  output:Seq[String] = Seq(""),

  size:Long = Long.MaxValue,
  limit:Long = Long.MaxValue,  
  delimiter:String = "\n", // use "" for http call  
  // Exception in thread "main" akka.stream.scaladsl.Framing$FramingException: Read 1048858 bytes which is more than 1048576 without seeing a line terminator
  // It does not affect: akka.http.parsing.max-chunk-size = 1m
  buffer:Int = 5 * 1024*1024,   
  format:String = "",
  
  entity:Seq[String] = Seq("ethereum:tx.extractor","arbitrum:tx.extractor"),
  
  datastore:String = "cache://", 
  cacheFlush:Long = 5000L,
  threadPool:Int = 16,
  timeout:Long = 5000L,
  timeoutIdle:Long = 60000L,// websocket idle timeout
  throttle:Long = 3000L,    // throttle for stat aggregation
  freq: Long = 3000L,       // Websocket freq update. Makes sense to be close to throttle

  cmd:String = "stream",
  params: Seq[String] = Seq(),
  sinks:Seq[String] = Seq()
)


object App extends skel.Server {
  
  def main(args:Array[String]):Unit = {
    Console.err.println(s"args: '${args.mkString(",")}'")

    val d = Config()

    val c = Configuration.withPriority(Seq(
      new ConfigurationAkka,
      new ConfigurationProp,
      new ConfigurationEnv, 
      new ConfigurationArgs(args,"trunk-stat","",
        ArgString('h', "http.host",s"listen host (def: ${d.host})"),
        ArgInt('p', "http.port",s"listern port (def: ${d.port})"),
        ArgString('u', "http.uri",s"api uri (def: ${d.uri})"),

        ArgLong('_', "cache.flush",s"Cache flush interval, msec (def: ${d.cacheFlush})"),
        ArgInt('_', "thread.pool",s"Thread pool for Websockets (def: ${d.threadPool})"),
        ArgLong('_', "freq",s"Websocket Listeners update frequency, msec (def=${d.freq}"),
        
        ArgString('f', "feed",s"Input Feed (def: ${d.feed})"),
        
        ArgString('o', "output",s"output (def=${d.output})"),        
        
        ArgString('e', "entity",s"Ingest entity: (block,transaction,tx,block-tx,transfer,log|event) def=${d.entity}"),

        ArgLong('_', "limit",s"Limit for entities to output (def=${d.limit})"),
        ArgLong('_', "size",s"Size limit for output (def=${d.size})"),        
        ArgString('_', "delimiter","""Delimiter characteds (def: '\n'). Usage example: --delimiter=`echo -e $"\r"` """),
        ArgInt('_', "buffer",s"Frame buffer (Akka Framing) (def: ${d.buffer})"),
        ArgLong('_', "throttle",s"Throttle messages in msec (def: ${d.throttle})"),
        ArgString('_', "format",s"Format output (json,csv,log) (def=${d.format})"),

        ArgString('d', "datastore",s"datastore dir (def: ${d.datastore})"),
        
        ArgLogging(),
        ArgParam("<params>",""),

        ArgCmd("server",s"Odometer Server "),
        ArgCmd("stream",s"Stat pipeline (requires -e <entity>)"),        
        
      ).withExit(1)
    )).withLogging()

    val config = Config(
      host = c.getString("http.host").getOrElse(d.host),
      port = c.getInt("http.port").getOrElse(d.port),
      uri = c.getString("http.uri").getOrElse(d.uri),
      cacheFlush = c.getLong("cache.flush").getOrElse(d.cacheFlush),
      threadPool = c.getInt("thread.pool").getOrElse(d.threadPool),

      
      feed = c.getListString("feed",d.feed),
      output = c.getListString("output",d.output),
      entity = c.getListString("entity",d.entity),
            
      limit = c.getLong("limit").getOrElse(d.limit),
      size = c.getLong("size").getOrElse(d.size),
      freq = c.getLong("freq").getOrElse(d.freq),
      delimiter = c.getString("delimiter").getOrElse(d.delimiter),
      buffer = c.getInt("buffer").getOrElse(d.buffer),
      throttle = c.getLong("throttle").getOrElse(d.throttle),     
      format = c.getString("format").getOrElse(d.format),
      
      datastore = c.getString("datastore").getOrElse(d.datastore),
            
      cmd = c.getCmd().getOrElse(d.cmd),      
      params = c.getParams(),
    )

    Console.err.println(s"${config}")
    
    def stream(odometer:Option[OdoRoutes]) = {
      val pp:Seq[Pipeline[_,_,_]] = config.entity.zipWithIndex.map{ case(e,i) => 
        e.split(":").toList match {
          case chain :: "tx.extractor" :: Nil =>
            new PipelineTx(odometer,chain,config.feed.lift(i).getOrElse(""),config.output.lift(i).getOrElse(""),config)
          case "tx.extractor" :: Nil =>
            new PipelineTx(odometer,"ethereum",config.feed.lift(i).getOrElse(""),config.output.lift(i).getOrElse(""),config)
          case _ => 
            Console.err.println(s"Uknown entity: '${e}'");
            sys.exit(1)            
        }
      }

      // start all pipelines
      val ppr = pp.map( _.run())

      (ppr.head,Some(pp.head))
    }

    val (r,pp) = config.cmd match {
      case "stream" => 
        stream(None)

      case "server" => 
        val store = config.datastore.split("://").toList match {
          case "mysql" :: db :: Nil => new OdoStoreDB(c,s"mysql://${db}")
          case "postgres" :: db :: Nil => new OdoStoreDB(c,s"postgres://${db}")
          case "mysql" :: Nil => new OdoStoreDB(c,"mysql://mysql")
          case "postgres" :: Nil => new OdoStoreDB(c,"postgres://postgres")
          case "jdbc" :: Nil => new OdoStoreDB(c,"mysql://mysql")

          case "redis" :: uri :: Nil => new OdoStoreRedis(uri)
          case "redis" :: Nil => new OdoStoreRedis("redis://localhost:6379/0")

          case "dir" :: dir ::  _ => new OdoStoreDir(dir)
          case "mem" :: Nil | "cache" :: Nil => new OdoStoreMem()
          
          case _ => {
            Console.err.println(s"Unknown datastore: '${config.datastore}'")
            sys.exit(1)
          }
        }

        // redis does not actually need a cache
        val cache = new OdoStoreCache(store, freq = config.cacheFlush)

        val reg = OdoRegistry(cache)

        // Execution context
        implicit val ex: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(config.threadPool))
        
        val r1 = run( config.host, config.port,config.uri,c,
          Seq(
            (reg,"StatRegistry",(r, ac) => {
              val odometer = new OdoRoutes(r)(ac,
                io.syspulse.skel.odometer.Config(timeoutIdle = config.timeoutIdle,freq = config.freq)
              ,ex)
              stream(Some(odometer))
              odometer
            })
          )
        )
        
        (r1,r1)
      case _ => 
        Console.err.println(s"Unknown command: ${config.cmd}")    
        sys.exit(2)
    }
    
    Console.err.println(s"r=${r}")
    r match {
      case a:Awaitable[_] => {
        val rr = Await.result(a,FiniteDuration(300,TimeUnit.MINUTES))
        Console.err.println(s"rr: ${rr}")
      }
      case akka.NotUsed => 
      case _ =>
    }

    Console.err.println(s"Result: ${r}")
    // sys.exit(0)
  }
}