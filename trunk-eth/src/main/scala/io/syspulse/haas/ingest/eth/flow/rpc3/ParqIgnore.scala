package io.syspulse.haas.ingest.eth.flow.rpc3

import com.github.mjakubowski84.parquet4s.{ParquetRecordEncoder,ParquetSchemaResolver}

import io.syspulse.haas.ingest.eth.flow.rpc3.RpcTraceCall
import io.syspulse.haas.ingest.eth.flow.rpc3.EthRpcJson._
import io.syspulse.haas.ingest.eth.{MempoolTx,CallTrace}
import io.syspulse.haas.ingest.eth.MempoolJson._
import io.syspulse.haas.ingest.eth.CallTraceJson._

import io.syspulse.skel.serde.ParqIgnore

object ParqRcpTraceCall extends ParqIgnore[RpcTraceCall] 

// object ParqCallTrace extends ParqIgnore[CallTrace]

// object ParqMempoolTx extends ParqIgnore[MempoolTx]


