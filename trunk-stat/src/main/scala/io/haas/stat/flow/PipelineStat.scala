package io.haas.stat.flow

import com.typesafe.scalalogging.Logger
import spray.json._
import DefaultJsonProtocol._

import io.haas.ingest.eth.etl.TxJson
import io.haas.ingest.eth.etl.Tx

trait PipelineStat[E] {
  protected val log = Logger(s"${this}")
  
  import TxJson._

  def parseTx(data:String):Seq[Tx] = {
    if(data.isEmpty()) return Seq()

    try {
      // check it is JSON
      if(data.stripLeading().startsWith("{")) {
        val tx = data.parseJson.convertTo[Tx]
        Seq(tx)
      } else {
        // CSV is not supported !
        log.error(s"CSV is not supported for Tx: '${data}'")
        Seq()
      }
    } catch {
      case e:Exception => 
        log.error(s"failed to parse: '${data}'",e)
        Seq()
    }
  }  
}
