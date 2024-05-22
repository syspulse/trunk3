package io.syspulse.haas.ingest

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import java.util.concurrent.TimeUnit
import scala.concurrent.Awaitable
import scala.concurrent.{Await, ExecutionContext, Future}
import akka.actor.typed.scaladsl.Behaviors
import java.util.concurrent.Executors

import com.typesafe.scalalogging.Logger
import io.jvm.uuid._

import io.syspulse.skel
import io.syspulse.skel.util.Util
import io.syspulse.skel.config._

import io.syspulse.skel.ingest.flow.Pipeline
import io.syspulse.haas.ingest.eth
import io.syspulse.haas.ingest.icp
import io.syspulse.haas.ingest.starknet
import io.syspulse.haas.ingest.vechain
import io.syspulse.haas.ingest.stellar
import io.syspulse.haas.ingest.solana

import io.syspulse.haas.intercept.store._
import io.syspulse.haas.intercept.server.InterceptRoutes

object App extends skel.Server {
  
  def main(args:Array[String]):Unit = {
    //Console.err.println(s"args: '${args.mkString(",")}'")

    val d = Config()

    val c = Configuration.withPriority(Seq(
      new ConfigurationAkka,
      new ConfigurationProp,
      new ConfigurationEnv, 
      new ConfigurationArgs(args,"trunk-ingest","",
        ArgString('h', "http.host",s"listen host (def: ${d.host})"),
        ArgInt('p', "http.port",s"listern port (def: ${d.port})"),
        ArgString('u', "http.uri",s"api uri (def: ${d.uri})"),
        
        ArgString('f', "feed",s"Input Feed (def: ${d.feed})"),
        ArgString('_', "feed.tx",s"Tx Feed (def: ${d.feedTx})"),
        ArgString('_', "feed.block",s"Block Feed (def: ${d.feedBlock})"),
        ArgString('_', "feed.transfer",s"Token Transfer Feed (def: ${d.feedTransfer})"),
        ArgString('_', "feed.log",s"EventLog Feed (def: ${d.feedLog})"),
        ArgString('_', "feed.mempool",s"Mempool Feed (def: ${d.feedMempool})"),

        ArgString('o', "output",s"output (def=${d.output})"),        
        ArgString('_', "output.tx",s"Tx output (def: ${d.outputTx})"),
        ArgString('_', "output.block",s"Block output (def: ${d.outputBlock})"),
        ArgString('_', "output.transfer",s"Token Transfer output (def: ${d.outputTransfer})"),
        ArgString('_', "output.log",s"EventLog output (def: ${d.outputLog})"),
        ArgString('_', "output.mempool",s"Mempool output (def: ${d.outputMempool})"),

        ArgString('e', "entity",s"Ingest entity: (block,transaction,tx,block-tx,transfer,log|event) def=${d.entity}"),

        ArgLong('_', "limit",s"Limit for entities to output (def=${d.limit})"),
        ArgLong('_', "size",s"Size limit for output (def=${d.size})"),
        ArgLong('_', "freq",s"Frequency (def=${d.freq}"),
        ArgString('_', "delimiter","""Delimiter characteds (def: '\n'). Usage example: --delimiter=`echo -e $"\r"` """),
        ArgInt('_', "buffer",s"Frame buffer (Akka Framing) (def: ${d.buffer})"),
        ArgLong('_', "throttle",s"Throttle messages in msec (def: ${d.throttle})"),
        ArgString('_', "format",s"Format output (json,csv,log) (def=${d.format})"),
        ArgString('t', "filter",s"Filter (def='${d.filter}')"),
        
        // ArgString('_', "abi",s"directory with ABI jsons (format: NAME-0xaddress.json) (def=${d.abi}"),

        ArgString('d', "datastore",s"datastore dir (def: ${d.datastore})"),
        ArgString('d', "datastore.intercept",s"Intercept datastore dir (def: ${d.datastoreIntercept})"),

        ArgLong('_', "block.throttle",s"Throttle between block batches (e.g. (def: ${d.blockThrottle}))"),
        ArgString('_', "block",s"Ingest from this block (def: ${d.block})"),
        ArgString('_', "block.end",s"Ingest until this block (def: ${d.blockEnd})"),
        ArgInt('l', "lag",s"Blocks lag (def: ${d.blockLag})"),
        ArgInt('b', "batch",s"Blocks Batch (def: ${d.blockBatch})"),
        ArgInt('_', "reorg",s"Blocks Reorg depth (def: ${d.blockReorg})"),
        ArgInt('_', "block.limit",s"Block batch limit (def: ${d.blockLimit}))"),

        ArgInt('_', "receipt.batch",s"Receipt Batch size (def: ${d.receiptBatch})"),
        ArgLong('_', "receipt.throttle",s"Throttle between receipt batches in msec (def: ${d.receiptThrottle})"),
        ArgString('_', "receipt.request",s"Receipt request type [block,batch] (def: ${d.receiptRequest})"),
        ArgLong('_', "receipt.delay",s"Delay for Receipt request after block in msec (def: ${d.receiptDelay})"),

        ArgString('_', "api.token",s"API Token (def: ${d.apiToken})"),

        ArgString('a', "alert.output",s"Output for alerts (def=${d.alertOutput})"),
        
        ArgString('s', "script",s"Interceptor Script (def=${d.script})"),

        ArgString('_', "rpc.url",s"RPC Url (optional) (def=${d.rpcUrl})"),

        ArgLong('_', "timeout.idle",s"Idle timeout in msec (def: ${d.timeoutIdle})"),
        ArgInt('_', "thread.pool",s"Thread pool for Websockets (def: ${d.threadPool})"),

        ArgLogging(),
        ArgParam("<params>",""),

        ArgCmd("server",s"Server"),
        ArgCmd("stream",s"Ingest pipeline (requires -e <entity>)"),        
        
      ).withExit(1)
    )).withLogging()

    val config = Config(
      host = c.getString("http.host").getOrElse(d.host),
      port = c.getInt("http.port").getOrElse(d.port),
      uri = c.getString("http.uri").getOrElse(d.uri),
      
      feed = c.getString("feed").getOrElse(d.feed),
      output = c.getString("output").getOrElse(d.output),
      entity = c.getListString("entity",d.entity),
      
      feedBlock = c.getString("feed.block").getOrElse(d.feedBlock),
      feedTransaction = c.getString("feed.transction").getOrElse(d.feedTransaction),
      feedTransfer = c.getString("feed.transfer").getOrElse(d.feedTransfer),
      feedLog = c.getString("feed.log").getOrElse(d.feedLog),
      feedMempool = c.getString("feed.mempool").getOrElse(d.feedMempool),
      feedTx = c.getString("feed.tx").getOrElse(d.feedTx),

      outputBlock = c.getString("output.block").getOrElse(d.outputBlock),
      outputTx = c.getString("output.tx").getOrElse(d.outputTx),
      outputTransfer = c.getString("output.transfer").getOrElse(d.outputTransfer),
      outputLog = c.getString("output.log").getOrElse(d.outputLog),
      outputMempool = c.getString("output.mempool").getOrElse(d.outputMempool),

      limit = c.getLong("limit").getOrElse(d.limit),
      size = c.getLong("size").getOrElse(d.size),
      freq = c.getLong("freq").getOrElse(d.freq),
      delimiter = c.getString("delimiter").getOrElse(d.delimiter),
      buffer = c.getInt("buffer").getOrElse(d.buffer),
      throttle = c.getLong("throttle").getOrElse(d.throttle),     
      format = c.getString("format").getOrElse(d.format),

      filter = c.getListString("filter",d.filter),
      //abi = c.getString("abi").getOrElse(d.abi),

      datastore = c.getString("datastore").getOrElse(d.datastore),
      datastoreIntercept = c.getString("datastore.intercept").getOrElse(d.datastoreIntercept),
                  
      blockThrottle = c.getLong("block.throttle").getOrElse(d.blockThrottle),      
      block = c.getString("block").getOrElse(d.block),
      blockEnd = c.getString("block.end").getOrElse(d.blockEnd),
      blockLag = c.getInt("lag").getOrElse(d.blockLag),
      blockBatch = c.getInt("batch").getOrElse(d.blockBatch),
      blockReorg = c.getInt("reorg").getOrElse(d.blockReorg),
      blockLimit = c.getInt("block.limit").getOrElse(d.blockLimit),
      
      receiptBatch = c.getInt("receipt.batch").getOrElse(d.receiptBatch),
      receiptThrottle = c.getLong("receipt.throttle").getOrElse(d.receiptThrottle),
      receiptRequest = c.getString("receipt.request").getOrElse(d.receiptRequest),
      receiptDelay = c.getLong("receipt.delay").getOrElse(d.receiptDelay),

      apiToken = c.getString("api.token").getOrElse(d.apiToken),

      alertOutput = c.getString("alert.output").getOrElse(d.alertOutput),
            
      script = c.getSmartString("script").getOrElse(d.script),

      rpcUrl = c.getSmartString("rpc.url").getOrElse(d.rpcUrl),

      timeoutIdle = c.getLong("timeout.idle").getOrElse(d.timeoutIdle),
      threadPool = c.getInt("thread.pool").getOrElse(d.threadPool),
      
      cmd = c.getCmd().getOrElse(d.cmd),      
      params = c.getParams(),
    )

    Console.err.println(s"${config}")

    def orf(config:Config,feed1:String,feed2:String,out1:String,out2:String) = {
      val c0 = config
      val c1 = if(feed1!="") c0.copy(feed = feed1) else c0.copy(feed = feed2)
      val c2 = if(out1!="") c1.copy(output = out1) else c1.copy(output = out2)
      c2
    }
    
    val (r,pp) = config.cmd match {
      case "server" => 
        val store = config.datastoreIntercept.split("://").toList match {
          case "mem" :: Nil | "cache" :: Nil => new ScriptStoreMem()
          
          case _ => {
            Console.err.println(s"Unknown datastore: '${config.datastoreIntercept}'")
            sys.exit(1)
          }
        }

        //val p = new eth.flow.rpc3.PipelineTxETL(orf(config,config.feedTransaction,config.feed,config.outputTransaction,config.output))
        val p = new eth.flow.rpc3.PipelineTx(orf(config,config.feedTransaction,config.feed,config.outputTransaction,config.output))

        // Execution context for Websocket
        val ex: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(config.threadPool))
        val r1 = run( config.host, config.port,config.uri,c,
          Seq(
            (InterceptRegistry(store),"InterceptRegistry",(r, ac) => {
              val intercept = new InterceptRoutes(r,p)(ac,config,ex)
              intercept
            })
          )
        )
        
        // start pipeline
        p.run()

        (r1,None)

      case "stream" => 
        val pp:Seq[PipelineIngest[_,_,_]] = config.entity.flatMap( e => e match {

          // ethereum_etl compatible input !
          case "block.etl" =>
            Some(new eth.flow.etl.PipelineBlock(orf(config,config.feedBlock,config.feed,config.outputBlock,config.output)))
          case "transaction.etl" =>
            Some(new eth.flow.etl.PipelineTransaction(orf(config,config.feedTransaction,config.feed,config.outputTx,config.output)))          
          case "transfer.etl" | "token.etl" =>
            Some(new eth.flow.etl.PipelineTokenTransfer(orf(config,config.feedTransfer,config.feed,config.outputTransfer,config.output)))
          case "log.etl" | "event.etl" => 
            Some(new eth.flow.etl.PipelineLog(orf(config,config.feedLog,config.feed,config.outputLog,config.output)))
          case "tx.etl" =>
            Some(new eth.flow.etl.PipelineTx(orf(config,config.feedTx,config.feed,config.outputTx,config.output)))
          
          // Lake format
          case "block.lake" =>
            Some(new eth.flow.lake.PipelineBlock(orf(config,config.feedBlock,config.feed,config.outputBlock,config.output)))
          case "transaction.lake" =>
            Some(new eth.flow.lake.PipelineTransaction(orf(config,config.feedTransaction,config.feed,config.outputTransaction,config.output)))
          case "transfer.lake" | "token.lake" =>
            Some(new eth.flow.lake.PipelineTokenTransfer(orf(config,config.feedTransfer,config.feed,config.outputTransfer,config.output)))
          case "log.lake" | "event.lake" =>
            Some(new eth.flow.lake.PipelineEvent(orf(config,config.feedLog,config.feed,config.outputLog,config.output)))
          case "tx.lake" =>
            Some(new eth.flow.lake.PipelineTx(orf(config,config.feedTx,config.feed,config.outputTx,config.output)))

          // Standard Web3 RPC 
          case "block" | "block.eth" =>
            Some(new eth.flow.rpc3.PipelineBlock(orf(config,config.feedBlock,config.feed,config.outputBlock,config.output)))
          case "tx" | "tx.eth" =>
            Some(new eth.flow.rpc3.PipelineTx(orf(config,config.feedTransaction,config.feed,config.outputTransaction,config.output)))
          case "transaction" | "transaction.eth" =>
            Some(new eth.flow.rpc3.PipelineTransaction(orf(config,config.feedTransaction,config.feed,config.outputTransaction,config.output)))
          case "log" | "event" | "log.eth" | "event.eth" =>
            Some(new eth.flow.rpc3.PipelineEvent(orf(config,config.feedTransaction,config.feed,config.outputTransaction,config.output)))
          case "transfer" | "token" | "transafer.eth" | "token.eth" =>
            Some(new eth.flow.rpc3.PipelineTokenTransfer(orf(config,config.feedTransaction,config.feed,config.outputTransaction,config.output)))
          case "tx.extractor" =>
            Some(new eth.flow.rpc3.PipelineTxETL(orf(config,config.feedTransaction,config.feed,config.outputTransaction,config.output)))
          

          // ICP Rosetta API
          case "block.icp.rosetta" =>
            Some(new icp.flow.rosetta.PipelineBlock(orf(config,config.feedBlock,config.feed,config.outputBlock,config.output)))
          case "transaction.icp.rosetta" | "tx.icp.rosetta" =>
            Some(new icp.flow.rosetta.PipelineTansaction(orf(config,config.feedBlock,config.feed,config.outputBlock,config.output)))

          // ICP Ledger API
          case "block.icp" =>
            Some(new icp.flow.ledger.PipelineBlock(orf(config,config.feedBlock,config.feed,config.outputBlock,config.output)))
          case "transaction.icp" | "tx.icp" =>
            Some(new icp.flow.ledger.PipelineTransaction(orf(config,config.feedBlock,config.feed,config.outputBlock,config.output)))

          // Starknet
          case "block.stark" =>
            Some(new starknet.flow.rpc.PipelineBlock(orf(config,config.feedBlock,config.feed,config.outputBlock,config.output)))
          case "transaction.stark" =>
            Some(new starknet.flow.rpc.PipelineTransaction(orf(config,config.feedTransaction,config.feed,config.outputTransaction,config.output)))
          case "tx.stark" =>
            Some(new starknet.flow.rpc.PipelineTx(orf(config,config.feedTransaction,config.feed,config.outputTx,config.output)))

          // Vechain
          case "block.vechain" =>
            Some(new vechain.flow.rpc.PipelineBlock(orf(config,config.feedBlock,config.feed,config.outputBlock,config.output)))
          case "transaction.vechain" =>
            Some(new vechain.flow.rpc.PipelineTransaction(orf(config,config.feedTransaction,config.feed,config.outputTransaction,config.output)))
          case "tx.vechain" =>
            Some(new vechain.flow.rpc.PipelineTx(orf(config,config.feedTransaction,config.feed,config.outputTx,config.output)))

          // Stellar
          case "block.stellar" =>
            Some(new stellar.flow.horizon.PipelineBlock(orf(config,config.feedBlock,config.feed,config.outputBlock,config.output)))
          case "transaction.stellar" =>
            Some(new stellar.flow.horizon.PipelineTransaction(orf(config,config.feedTransaction,config.feed,config.outputTransaction,config.output)))
          case "tx.stellar" =>
            Some(new stellar.flow.horizon.PipelineTx(orf(config,config.feedTransaction,config.feed,config.outputTransaction,config.output)))

          // Solana
          case "block.solana" =>
            Some(new solana.flow.rpc.PipelineBlock(orf(config,config.feedBlock,config.feed,config.outputBlock,config.output)))
          case "transaction.solana" =>
            Some(new solana.flow.rpc.PipelineTransaction(orf(config,config.feedTransaction,config.feed,config.outputTransaction,config.output)))

          // Ethereum mempool
          case "mempool" | "mempool.eth" => 
            Some(new eth.flow.rpc3.PipelineMempool(orf(config,config.feedMempool,config.feed,config.outputMempool,config.output)))
          case "mempool.ws" => 
            Some(new eth.flow.rpc3.PipelineMempoolStream(orf(config,config.feedMempool,config.feed,config.outputMempool,config.output)))
          case "trace" | "mempool.trace" => 
            Some(new eth.flow.rpc3.PipelineMempoolTrace(orf(config,config.feedMempool,config.feed,config.outputMempool,config.output)))

          case _ => 
            Console.err.println(s"Uknown entity: '${e}'");
            sys.exit(1)
            None
        })

        // start all pipelines
        val ppr = pp.map( _.run())

        (ppr.head,Some(pp.head))
                      
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

    Console.err.println(s"Result: ${pp.map(_.countObj)}")
    // sys.exit(0)
  }
}