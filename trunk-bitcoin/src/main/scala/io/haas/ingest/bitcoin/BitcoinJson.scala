package io.haas.ingest.bitcoin

import com.typesafe.scalalogging.Logger

import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

import io.syspulse.skel.service.JsonCommon

object BitcoinJson extends JsonCommon with NullOptions {  
  import DefaultJsonProtocol._
  
  implicit val jf_btc_trx: RootJsonFormat[Transaction] = jsonFormat13(Transaction)  
  implicit val jf_btc_block: RootJsonFormat[Block] = jsonFormat18(Block)  
  implicit val jf_btc_tx: RootJsonFormat[Tx] = jsonFormat14(Tx)
}
