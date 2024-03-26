package io.syspulse.haas.ingest.solana.flow.rpc

import com.typesafe.scalalogging.Logger

import io.syspulse.skel.Ingestable

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

case class RpcStatus(
  `Ok`: Option[String],
  `Err`: Option[String]
)

case class RpcMeta(
  computeUnitsConsumed: Long,
  err: Option[String],
  fee: Long,
  innerInstructions: Array[String],
  loadedAddresses: RpcLoadedAddresses,
  logMessages: Array[String],
  postBalances: Array[String],
  postTokenBalances: Array[String],
  preTokenBalances: Array[String],
  rewards: Option[Array[RpcReward]],
  status: RpcStatus
) 

case class RpcHeader(
  numReadonlySignedAccounts: Int,
  numReadonlyUnsignedAccounts: Int,
  numRequiredSignatures: Int,
)

case class RpcInstructions(
  acconts: Array[Long],
  data: String,
  programIdIndex: Long,
  stackHeight: Option[Long],
)


case class RpcMessage(
  accountKeys: Array[String],
  header: RpcHeader,
  instructions: Array[RpcInstructions],
  recentBlockhash: String
)

case class RpcTransactionTx(
  message: RpcMessage,  
  signatures: Array[String]
)

case class RpcTransaction(
  meta: RpcMeta,
  transaction: RpcTransactionTx,

  version: String

  // block_number:Option[Long] = None, // NOT FROM RPC !!! used internally for streaming Block timestamp 
  // timestamp:Option[Long] = None // NOT FROM RPC !!! used internally for streaming Block timestamp 
)  extends Ingestable


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
  id: Any
)
