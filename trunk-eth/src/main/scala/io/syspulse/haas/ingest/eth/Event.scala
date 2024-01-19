package io.syspulse.haas.ingest.eth

import io.syspulse.skel.Ingestable

case class Event(
  ts:Long,
  block:Long,
  contract:String,
  data:String,
  hash:String,   // transaction hash !
  topics:Seq[String] = Seq(), 
  i:Int,         // log index
  tix:Int        // transaction index in block
  
) extends Ingestable {
  override def getKey:Option[Any] = Some(hash)
}

// used only in Fat Tx
case class EventTx(
  i:Int,         // log index
  contract:String,
  data:String,  
  topics:Seq[String] = Seq(), 
) extends Ingestable
