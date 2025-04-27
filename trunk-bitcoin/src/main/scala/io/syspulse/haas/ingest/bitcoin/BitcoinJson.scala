package io.syspulse.haas.ingest.bitcoin

import com.typesafe.scalalogging.Logger

import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

import io.syspulse.skel.service.JsonCommon

object BitcoinJson extends DefaultJsonProtocol //with JsonCommon  
{
  import DefaultJsonProtocol._
  
  implicit val jf_btc_trx = jsonFormat13(Transaction)
  implicit val jf_btc_block = jsonFormat18(Block)  
  implicit val jf_btc_tx = jsonFormat14(Tx)   
}
