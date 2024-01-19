package io.syspulse.haas.serde

import scala.jdk.CollectionConverters._

import scala.util.Random

import spray.json._
import DefaultJsonProtocol._

import io.syspulse.haas.core.{Token,TokenBlockchain,TokenLock,TokenLocks}

object TokenJson extends DefaultJsonProtocol {
  
  implicit val jf_tok_tb = jsonFormat2(TokenBlockchain.apply _)
  implicit val jf_tok_lock = jsonFormat2(TokenLock.apply _)
  implicit val jf_tok_locks = jsonFormat2(TokenLocks.apply _)
  implicit val jf_tok_tok = jsonFormat12(Token.apply _)
}
