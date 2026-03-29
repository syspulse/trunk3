package io.haas.ingest.eth.flow

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util

package object etl {
  type Block = io.haas.ingest.ext.Block
  type Log = io.haas.ingest.ext.Log
  type Tx = io.haas.ingest.ext.Tx 
}