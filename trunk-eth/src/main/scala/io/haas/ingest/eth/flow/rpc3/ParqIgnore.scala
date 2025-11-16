package io.haas.ingest.eth.flow.rpc3

import com.github.mjakubowski84.parquet4s.{ParquetRecordEncoder,ParquetSchemaResolver}

import io.haas.ingest.eth.flow.rpc3.RpcTraceCall
import io.haas.ingest.eth.flow.rpc3.EthRpcJson._
import io.haas.ingest.eth.{MempoolTx,CallTrace}
import io.haas.ingest.eth.MempoolJson._
import io.haas.ingest.eth.CallTraceJson._

import io.syspulse.skel.serde.ParqIgnore

object ParqRcpTraceCall extends ParqIgnore[RpcTraceCall] 

// object ParqCallTrace extends ParqIgnore[CallTrace]

// object ParqMempoolTx extends ParqIgnore[MempoolTx]


