package io.syspulse.haas.ingest.eth.etl

import io.syspulse.skel.Ingestable

// used only in Fat Tx
case class LogTx(
  index:Int,
  address:String,
  data:String,  
  topics:Array[String] = Array(), 
) extends Ingestable
