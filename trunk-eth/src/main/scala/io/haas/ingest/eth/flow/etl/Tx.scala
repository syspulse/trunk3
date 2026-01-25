package io.haas.ingest.eth

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

package object etl {
  type Block = io.haas.ingest.ext.BlockExt
  type LogTx = io.haas.ingest.ext.LogExt
  type Tx = io.haas.ingest.ext.TxExt  
}