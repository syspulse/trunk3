package io.syspulse.haas.ingest.icp

import com.typesafe.scalalogging.Logger

import io.syspulse.skel.service.JsonCommon
import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}

import io.syspulse.skel.Ingestable


object IcpJson extends DefaultJsonProtocol //with NullOptions 
{  
  //implicit val jf_icp_op = jsonFormat7(Operation)
  implicit val jf_icp_tx = jsonFormat15(Transaction)
  implicit val jf_icp_block = jsonFormat5(Block)   
}
