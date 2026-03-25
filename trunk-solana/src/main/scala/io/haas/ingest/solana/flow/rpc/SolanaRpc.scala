package io.haas.ingest.solana.flow.rpc

import com.typesafe.scalalogging.Logger

import io.syspulse.skel.Ingestable
import spray.json.JsArray
import spray.json.JsObject
import spray.json.JsValue

import com.github.mjakubowski84.parquet4s.{BinaryValue,NullValue,SchemaDef,TypedSchemaDef,ValueCodecConfiguration,ValueEncoder}
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BINARY

// {
//   "jsonrpc": "2.0",
//   "result": {
//     "blockHeight": 263194764,
//     "blockTime": 1706128394,
//     "blockhash": "68ZsFUDziKgiuKjsixtNPNp2eCrBwbKpiyV5SG6RWdhM",
//     "parentSlot": 274892481,
//     "previousBlockhash": "7a2uyhzmvHjKmL1srRaCmc3vgiL1L1ut4UgViAu77idu",
//     "transactions": [
//       {
//         "meta": {
//           "computeUnitsConsumed": 46402,
//           "err": null,
//           "fee": 6600,
//           "innerInstructions": [],
//           "loadedAddresses": {
//             "readonly": [],
//             "writable": []
//           },
//           "logMessages": [
//             "Program gSbePebfvPy7tRqimPoVecS2UsBvYv46ynrzWocc92s invoke [1]",            
//           ],
//           "postBalances": [
//             358084324260            
//           ],
//           "postTokenBalances": [],
//           "preBalances": [
//             358084330860            
//           ],
//           "preTokenBalances": [],
//           "rewards": null,
//           "status": {
//             "Ok": null
//           }
//         },
//         "transaction": {
//           "message": {
//             "accountKeys": [
//               "5U3bH5b6XtG99aVWLqwVzYPVpQiFHytBD68Rz2eFPZd7"              
//             ],
//             "header": {
//               "numReadonlySignedAccounts": 0,
//               "numReadonlyUnsignedAccounts": 3,
//               "numRequiredSignatures": 1
//             },
//             "instructions": [
//               {
//                 "accounts": [
//                   0,
//                   9,
//                   12
//                 ],
//                 "data": "6mJFQCt94hG4CKNYKgVcwfCCt4njTkQzbDCHjcvjpDqH9VathnSJ3q",
//                 "programIdIndex": 13,
//                 "stackHeight": null
//               }              
//             ],
//             "recentBlockhash": "HNG5XXRhguYh4bNe9HEzrDYW7jpnMEjBy1BHncSkeGkt"
//           },
//           "signatures": [
//             "5KdMUEJaK8ZHrR7SnZwoWKxVK993QseFgHmtTm549rg1bfJ6hzs5TNuerYivfaTUAy1JSk2KY6o3T7oS5JDHsBmX"
//           ]
//         },
//         "version": "legacy"
//       }      
//     ]
//   },
//   "id": 1
// }


case class RpcLoadedAddresses(
  readonly: Array[String],
  writable: Array[String]
)

// {                     
//   "commission": null,
//   "lamports": -46,   
//   "postBalance": 472428,
//   "pubkey": "Gj5XDSQJFsiknh86EkvW8vQ7tWZz1ver6iLtvFY8bdne",
//   "rewardType": "Rent"                                                                                          
// }
case class RpcReward(
  commission: Option[Long],
  lamports:Long,
  postBalance: Long,
  pubkey: String,
  rewardType:String
)

// "err": {
//   "InstructionError": [
//     1,
//     {
//       "Custom": 6003
//     }
//   ]
// },

// "status": {
//   "Err": {
//     "InstructionError": [
//       1,
//       {
//         "Custom": 6003
//       }
//     ]
//   }
// }

// "status": {
//   "Ok": null
// }

case class RpcErr(
  `InstructionError`:Option[JsArray]
)

case class RpcStatus(
  `Ok`: Option[JsValue] = None,
  `Err`: Option[JsValue] = None
)

case class RpcInnerInstruction(
  index: Int,
  instructions: Array[RpcInstruction]
)

case class RpcUiTokenAmount(
  amount: String,
  decimals: Int,
  uiAmount: Double,
  uiAmountString: String
)

case class RpcPostTokenBalance(
  accountIndex: Long,
  mint: String,
  owner: String,
  programId: String,
  uiTokenAmount: RpcUiTokenAmount
)

// case class RpcError(
//   `Err`: Option[JsObject]
// )

case class RpcMeta(
  computeUnitsConsumed: Long,
  err: Option[JsValue],
  fee: Long,
  innerInstructions: Option[Array[RpcInnerInstruction]],
  loadedAddresses: Option[RpcLoadedAddresses],
  logMessages: Option[Array[String]],
  postBalances: Array[Long],
  postTokenBalances: Array[RpcPostTokenBalance],
  preTokenBalances: Array[RpcPostTokenBalance],
  rewards: Option[Array[RpcReward]],
  status: RpcStatus
) 

case class RpcHeader(
  numReadonlySignedAccounts: Int,
  numReadonlyUnsignedAccounts: Int,
  numRequiredSignatures: Int,
)

case class RpcParsedInstruction(
  info: Option[JsObject],
  `type`: String
)

case class RpcInstruction(
  accounts: Seq[String],
  data: Option[String],
  programIdIndex: Option[Long],
  programId: Option[String],
  program: Option[String],
  parsed: Option[RpcParsedInstruction],
  stackHeight: Option[Long],
)

case class RpcMessage(
  accountKeys: Array[JsValue],
  header: Option[RpcHeader],
  instructions: Array[RpcInstruction],
  recentBlockhash: String
)

case class RpcTransactionTx(
  message: RpcMessage,  
  signatures: Array[String]
)

case class RpcTransaction(
  meta: RpcMeta,
  transaction: RpcTransactionTx,

  // Solana RPC returns `"legacy"` as a string or a numeric value for newer transaction versions (e.g. 0).
  // Keep it as JsValue to avoid fragile implicit coercions during spray-json parsing.
  version: Option[JsValue] = None

  // block_number:Option[Long] = None, // NOT FROM RPC !!! used internally for streaming Block timestamp 
  // timestamp:Option[Long] = None // NOT FROM RPC !!! used internally for streaming Block timestamp 
)  extends Ingestable


// SlotHeight	Latest network progress / time	Sync status, RPC freshness
// BlockHeight	Latest confirmed block count	Chain history, indexing
// Explorers show SlotHeight as block number
case class RpcBlock(
  blockHeight: Long,
  blockTime: Long,
  blockhash: String,
  parentSlot: Long,
  previousBlockhash: String,
  
  transactions: Array[RpcTransaction],  
  
) extends Ingestable

case class RpcBlockResult(  
  jsonrpc:String,  
  result:Option[RpcBlock],
  id: JsValue
)


// ========================================================================================
// Parq ingorers for nested types
// ========================================================================================
object RpcInstruction {
  // Keep schema simple: instruction is stored as a nullable string blob.
  implicit val parquetValueEncoder: ValueEncoder[RpcInstruction] =
    (i: RpcInstruction, _: ValueCodecConfiguration) => NullValue
      //if (i == null) NullValue else BinaryValue(i.toString)

  implicit val parquetSchema: TypedSchemaDef[RpcInstruction] =
    SchemaDef.primitive(BINARY, required = false).typed[RpcInstruction]
}

object RpcParsedInstruction {
  implicit val parquetValueEncoder: ValueEncoder[RpcParsedInstruction] =
    (p: RpcParsedInstruction, _: ValueCodecConfiguration) => NullValue
      //if (p == null) NullValue else BinaryValue(p.toString)

  implicit val parquetSchema: TypedSchemaDef[RpcParsedInstruction] =
    SchemaDef.primitive(BINARY, required = false).typed[RpcParsedInstruction]
}
