package io.haas.ingest.solana

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util
import io.haas.ingest.solana.flow.rpc.RpcInstruction

case class Transaction(  
  ts:Option[Long],          // timestamp  
  b:Option[Long],           // block number (parent slot - 1)   
  h:Option[Long],           // block height  
  
  acc:Array[String],        // account key
  unts:Long,                // consumed units
  fee:Long,                 // fee (lamports)

  ins:Array[RpcInstruction], // instructions
  logs:Array[String],        // log messages

  sig:String,                // transaction signature 

  sta:String,                // status
  ver:String,                // version
  i:Option[Long] = None,  // transaction index in Block
  
) extends Ingestable {
  override def getKey:Option[Any] = Some(sig)

  override def toString = Util.toStringWithArray(this)
}


// ======================================================
// ATTENTION: This code kills the compiler (It hangs!)
// ======================================================
// object Transaction {
//   /** Parquet4s encodes [[Block]] field `tx` via shapeless; nested [[Transaction]] / [[RpcInstruction]] is not supported.
//     * This pair always writes SQL NULL and uses an optional BINARY column so generic [[ParquetRecordEncoder]] still applies.
//     */
//   implicit val parquetIgnoreOptionArrayTransaction: ValueEncoder[Option[Array[Transaction]]] =
//     (_: Option[Array[Transaction]], _: ValueCodecConfiguration) => NullValue
//   implicit val parquetIgnoreOptionArrayTransactionSchema: TypedSchemaDef[Option[Array[Transaction]]] =
//     SchemaDef.primitive(BINARY, required = false).typed[Option[Array[Transaction]]]
// }
