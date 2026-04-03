package io.haas.ingest.solana

import io.syspulse.skel.Ingestable
import io.syspulse.skel.util.Util
import io.haas.ingest.solana.flow.rpc.RpcInstruction

// Compact token balance representation for Transaction meta fields
case class TokUI(
  v: BigInt,                 // amount
  dec: Int,                  // decimals
  vu: Option[Double] = None, // uiAmount (nullable in RPC)
  vs: Option[String] = None, // uiAmountString
)

case class TokBal(
  i: Int,          // accountIndex
  pid: String,      // programId
  ui: TokUI,        // uiTokenAmount
  mint: String,     // mint address
  own: String
)

case class Transaction(  
  ts:Option[Long],          // timestamp  
  b:Option[Long],           // block number (parent slot - 1)   
  h:Option[Long],           // block height  
  
  acc:Array[String],        // account key
  unts:Long,                // consumed units (legacy)
  fee:Long,                 // fee (lamports)
  err:Option[String] = None,// error (stringified json if present)
  used:Long = 0L,           // computeUnitsConsumed
  cost:Long = 0L,           // costUnits
  bal1:Option[Array[Long]] = None,          // postBalances
  bal0:Option[Array[Long]] = None,          // preBalances
  tok0:Option[Array[TokBal]] = None,        // postTokenBalances
  tok1:Option[Array[TokBal]] = None,        // preTokenBalances

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
// ATTENTION: This Parquet4s code kills the compiler (It hangs!)
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
