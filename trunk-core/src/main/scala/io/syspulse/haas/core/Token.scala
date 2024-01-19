package io.syspulse.haas.core

import scala.jdk.CollectionConverters._
import io.syspulse.skel.Ingestable

case class TokenBlockchain(
  bid:Blockchain.ID,
  addr:String
)

case class TokenLock(
  addr:String,
  tag:String
)

case class TokenLocks(
  bid:Blockchain.ID,
  lock:Seq[TokenLock]
)

case class Token(
  id:Token.ID, 
  symbol:String, 
  name:String, 
  addr:Option[String] = None,   // default primary address 
  cat:List[String] = List(), 
  icon:Option[String] = None, 
  src:Option[Long] = None,
  dcml:Option[Int] = None,

  chain:Seq[TokenBlockchain] = Seq(),
  locks:Seq[TokenLocks] = Seq(),
    
  ts:Long = 0L,
  ts0:Long = 0L,
) extends Ingestable {
  override def getKey:Option[Any] = Some(id)
}

object Token {
  type ID = String
}