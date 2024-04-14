package io.syspulse.haas.stat

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import java.util.concurrent.TimeUnit
import scala.concurrent.Awaitable
import scala.concurrent.{Await, ExecutionContext, Future}
import akka.actor.typed.scaladsl.Behaviors

import com.typesafe.scalalogging.Logger
import io.jvm.uuid._

import io.syspulse.skel
import io.syspulse.skel.ingest.flow.Pipeline
import io.syspulse.skel.util.Util
import io.syspulse.skel.config._

import io.syspulse.haas.stat.flow._
import io.syspulse.haas.ingest.eth

case class Config(  
  // host:String="0.0.0.0",
  // port:Int=8080,
  // uri:String = "/api/v1/trunk",
  
  feed:String = "",
  output:String = "",

  feedBlock:String = "",
  feedTransaction:String = "",
  feedTransfer:String = "",
  feedLog:String = "",
  feedMempool:String = "",
  feedTx:String = "",
  
  size:Long = Long.MaxValue,
  limit:Long = Long.MaxValue,
  freq: Long = 0L,
  delimiter:String = "\n", // use "" for http call  
  // Exception in thread "main" akka.stream.scaladsl.Framing$FramingException: Read 1048858 bytes which is more than 1048576 without seeing a line terminator
  // It does not affect: akka.http.parsing.max-chunk-size = 1m
  buffer:Int = 5 * 1024*1024, 
  throttle:Long = 0L,  
  format:String = "",
  
  entity:Seq[String] = Seq("tx.extractor"),
  
  datastore:String = "", // store directory root

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
        // ArgString('h', "http.host",s"listen host (def: ${d.host})"),
        // ArgInt('p', "http.port",s"listern port (def: ${d.port})"),
        // ArgString('u', "http.uri",s"api uri (def: ${d.uri})"),
        
        ArgString('f', "feed",s"Input Feed (def: ${d.feed})"),
        ArgString('_', "feed.tx",s"Tx Feed (def: ${d.feedTx})"),
        ArgString('_', "feed.block",s"Block Feed (def: ${d.feedBlock})"),
        ArgString('_', "feed.transfer",s"Token Transfer Feed (def: ${d.feedTransfer})"),
        ArgString('_', "feed.log",s"EventLog Feed (def: ${d.feedLog})"),
        ArgString('_', "feed.mempool",s"Mempool Feed (def: ${d.feedMempool})"),

        ArgString('o', "output",s"output (def=${d.output})"),        
        
        ArgString('e', "entity",s"Ingest entity: (block,transaction,tx,block-tx,transfer,log|event) def=${d.entity}"),

        ArgLong('_', "limit",s"Limit for entities to output (def=${d.limit})"),
        ArgLong('_', "size",s"Size limit for output (def=${d.size})"),
        ArgLong('_', "freq",s"Frequency (def=${d.freq}"),
        ArgString('_', "delimiter","""Delimiter characteds (def: '\n'). Usage example: --delimiter=`echo -e $"\r"` """),
        ArgInt('_', "buffer",s"Frame buffer (Akka Framing) (def: ${d.buffer})"),
        ArgLong('_', "throttle",s"Throttle messages in msec (def: ${d.throttle})"),
        ArgString('_', "format",s"Format output (json,csv,log) (def=${d.format})"),

        ArgString('d', "datastore",s"datastore dir (def: ${d.datastore})"),
        
        ArgLogging(),
        ArgParam("<params>",""),

        // ArgCmd("server",s"Server"),
        ArgCmd("stream",s"Stat pipeline (requires -e <entity>)"),        
        
      ).withExit(1)
    )).withLogging()

    val config = Config(
      // host = c.getString("http.host").getOrElse(d.host),
      // port = c.getInt("http.port").getOrElse(d.port),
      // uri = c.getString("http.uri").getOrElse(d.uri),
      
      feed = c.getString("feed").getOrElse(d.feed),
      output = c.getString("output").getOrElse(d.output),
      entity = c.getListString("entity",d.entity),
      
      feedBlock = c.getString("feed.block").getOrElse(d.feedBlock),
      feedTransaction = c.getString("feed.transction").getOrElse(d.feedTransaction),
      feedTransfer = c.getString("feed.transfer").getOrElse(d.feedTransfer),
      feedLog = c.getString("feed.log").getOrElse(d.feedLog),
      feedMempool = c.getString("feed.mempool").getOrElse(d.feedMempool),
      feedTx = c.getString("feed.tx").getOrElse(d.feedTx),
      
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
    
    val (r,pp) = config.cmd match {
      case "stream" => {
        val pp:Seq[Pipeline[_,_,_]] = config.entity.map( e => e match {

          case "tx.extractor" =>
            new PipelineTx(config)
          
          case _ => 
            Console.err.println(s"Uknown entity: '${e}'");
            sys.exit(1)            
        })

        // start all pipelines
        val ppr = pp.map( _.run())

        (ppr.head,Some(pp.head))
                
      }       
    }
    
    Console.err.println(s"r=${r}")
    r match {
      case a:Awaitable[_] => {
        val rr = Await.result(a,FiniteDuration(300,TimeUnit.MINUTES))
        Console.err.println(s"rr: ${rr}")
      }
      case akka.NotUsed => 
    }

    Console.err.println(s"Result: ${pp.map(_.countObj)}")
    // sys.exit(0)
  }
}