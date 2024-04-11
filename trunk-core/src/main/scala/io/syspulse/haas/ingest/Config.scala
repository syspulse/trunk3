package io.syspulse.haas.ingest

import com.typesafe.scalalogging.Logger

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

  outputBlock:String = "",
  outputTransaction:String = "",
  outputTransfer:String = "",
  outputLog:String = "",
  outputMempool:String = "",
  outputTx:String = "",
  
  abi:String = "abi/",
  source:String="",
  
  size:Long = Long.MaxValue,
  limit:Long = Long.MaxValue,
  freq: Long = 0L,
  delimiter:String = "\n", // use "" for http call  
  // Exception in thread "main" akka.stream.scaladsl.Framing$FramingException: Read 1048858 bytes which is more than 1048576 without seeing a line terminator
  // It does not affect: akka.http.parsing.max-chunk-size = 1m
  buffer:Int = 5 * 1024*1024, 
  throttle:Long = 5000L,  
  format:String = "",
  
  entity:Seq[String] = Seq("block"),
  
  expr:String = "",
  
  datastore:String = "", // store directory root

  filter:Seq[String] = Seq(),

  ingestCron:String = "12", // 12 seconds

  block:String = "latest", // which block to use (only for http:// RPC source)
  blockEnd:String = "",    // empty is infinite
  blockBatch:Int = 10,     // batch size (how many blocks to ask)
  blockThrottle:Long = 1L, // when chasing to latest block in batches
  blockLag:Int = 0, // lag
  blockReorg:Int = 0, // number of block reorgs

  receiptBatch:Int = -1,      // max receipt batch size
  receiptThrottle:Long = 1000L, // throttle between receipts batches within the single block
  receiptRequest:String = "block", // receipts request style
  receiptDelay:Long = 0L,          // delay to request receipts (Polygon/BSC lag)

  apiToken:String = "",

  alertOutput:String = "null://",
  interceptorName:String = "Interceptor",
  interceptorSid:String = "trunk:interceptor",
  interceptorCat:String = "EVENT",
  interceptorType:String = "monitor",
  interceptorSeverity:Double = 0.15,
  interceptorBlockchain:String = "ethereum",
  interceptorContract:String = "0x0000000000000000000000000000000000000007",
  script:String = "",

  rpcUrl:String = "", // optional RPC url

  cmd:String = "stream",
  params: Seq[String] = Seq(),
  sinks:Seq[String] = Seq()
)
