package io.syspulse.haas.ingest.starknet.flow.rpc

import com.typesafe.scalalogging.Logger

import io.syspulse.skel.Ingestable

// {
//   "jsonrpc": "2.0",
//   "id": 1,
//   "result": {
//     "block_hash": "0x5be1e82035253b9ca9198dfa6803f9ecbd8f5b112eaebeb41c5fc6604548322",
//     "block_number": 458938,
//     "new_root": "0x51c4ea80fb7abba474ef1793e0cd104e5d21e102ef532bc35a158997a5fc770",
//     "parent_hash": "0xffea266e020fcceca1ec054848c726494f31741b7be0e41b457aacc7f36ac8",
//     "sequencer_address": "0x1176a1bd84444c89232ec27754698e5d2e7e1a7f1539f12027f28b23ec9f3d8",
//     "status": "ACCEPTED_ON_L2",
//     "timestamp": 1701961581,
//     "transactions": [
//       {
//         "calldata": [
//           "0x2",
//           "0x53c91253bc9682c04929ca02ed00b3e423f6710d2ee7e0d5ebb06f3ecf368a8",
//           "0x219209e083275171774dab1df80982e9df2096516f06319c5c6d71ae0a8480c",
//           "0x3",
//           "0x5dcd26c25d9d8fd9fc860038dcb6e4d835e524eb8a85213a8cda5b7fff845f6",
//           "0xf4240",
//           "0x0",
//           "0x5dcd26c25d9d8fd9fc860038dcb6e4d835e524eb8a85213a8cda5b7fff845f6",
//           "0x2f0b3c5710379609eb5495f1ecd348cb28167711b73609fe565a72734550354",
//           "0x3",
//           "0x2e371bb65eb13741ea9378d1732af10ad601d1aaf0a5ab325a7058aa026d54b",
//           "0xf4240",
//           "0x0"
//         ],
//         "max_fee": "0xa7a5f5df6aee0",
//         "nonce": "0x51",
//         "sender_address": "0x2e371bb65eb13741ea9378d1732af10ad601d1aaf0a5ab325a7058aa026d54b",
//         "signature": [
//           "0x7bf650782b51f7e04dc0586c26c0dbb5a30cf581dfd390c3bc30c5e52a548ea",
//           "0x4a2f3849e1b1697d627dd6a8506f60666abd87e0277fca1ef71fed8627ad3f1"
//         ],
//         "transaction_hash": "0x49080887e20ac4668d5a7687616b23ef9ef4e9cc81a2b82579d55d090a5d3d4",
//         "type": "INVOKE",
//         "version": "0x1"
//       },
//     }
//   }

case class RpcTx(
  calldata: Option[Array[String]],
  max_fee: Option[String],
  nonce: String,
  sender_address: Option[String],
  signature: Option[Array[String]],
  transaction_hash: String,
  `type`: String,
  version: String,
  entry_point_selector:Option[String],

  block_number:Option[Long] = None, // NOT FROM RPC !!! used internally for streaming Block timestamp 
  timestamp:Option[Long] = None // NOT FROM RPC !!! used internally for streaming Block timestamp 
)  extends Ingestable

case class RpcUncle(
  hash:String
)

// "l1_gas_price":{"price_in_wei":"0xc203ba11b"}
case class RpcL1Gas( 
  price_in_wei:String
)
case class RpcBlock(  
  block_hash: String,
  block_number: Long,
  new_root: String,
  parent_hash: String,
  sequencer_address: String,
  status: String,
  timestamp: Long,
  transactions: Array[RpcTx],  

  l1_gas_price:Option[RpcL1Gas]
) extends Ingestable

case class RpcBlockResult(  
  jsonrpc:String,  
  result:Option[RpcBlock],
  id: Any
)

// {
//   "data": [
//     "0x43b6e05126164f05187d24b467a36204fbadf23fd74445ea595cfd0f21b194e",
//     "0x4b3802058cdd4fc4e352e866e2eef5abde7d62e78116ac68b419654cbebc021",
//     "0x3971ece",
//     "0x0"
//   ],
//   "from_address": "0x53c91253bc9682c04929ca02ed00b3e423f6710d2ee7e0d5ebb06f3ecf368a8",
//   "keys": [
//     "0x99cd8bde557814842a3121e8ddfd433a539b8c9f14bf31ebf108d12e6196e9"
//   ]
// }
case class RpcEvent(  
  data: Array[String],  
  from_address: String,
  keys: Array[String]
)

// "actual_fee": "0x6e6e43922e400",
// "block_hash": "0x2cb295c4d9377617397f1ba7684f8c0dbc19b1f70360dafe39492608886dfa8",
// "block_number": 458939,
// "events": [
case class RpcEvents(  
  actual_fee: String,
  block_hash: String,
  block_number: Long,
  events: Array[String]
)

case class RpcEventsResult(  
  jsonrpc:String,  
  result:Option[RpcEvents],
  id: Any
)

// -------------------------------------------------------------------------------------
// {
//   "actual_fee": "0x6e6e43922e400",                                                                                           
//   "block_hash": "0x2cb295c4d9377617397f1ba7684f8c0dbc19b1f70360dafe39492608886dfa8",
//   "block_number": 458939,
//   "events": [
//     {                                                                                                                        
//       "data": [
//         "0x43b6e05126164f05187d24b467a36204fbadf23fd74445ea595cfd0f21b194e",
//         "0x4b3802058cdd4fc4e352e866e2eef5abde7d62e78116ac68b419654cbebc021",
//         "0x3971ece",
//         "0x0"
//       ],       
//       "from_address": "0x53c91253bc9682c04929ca02ed00b3e423f6710d2ee7e0d5ebb06f3ecf368a8",
//       "keys": [                                                                                                              
//         "0x99cd8bde557814842a3121e8ddfd433a539b8c9f14bf31ebf108d12e6196e9"
//       ]      
//     },
//     ...
//   ],    
//   "execution_status": "SUCCEEDED",
//   "finality_status": "ACCEPTED_ON_L2",
//   "messages_sent": [],
//   "transaction_hash": "0x7c2864fc1527193bf8c47b9d750d36003f42892ebd7df0c0e099024a32b62f9",
//   "type": "INVOKE"
// }


case class RcpReceiptMessageL1(
  from_address:String,
  to_address:String,
  payload:Array[String]
)

case class RpcReceipt(
  actual_fee: String,
  block_hash: String,
  block_number: Long,
  events: Array[RpcEvent],    
  execution_status: String,
  finality_status: String,
  messages_sent: Array[RcpReceiptMessageL1],
  transaction_hash: String,
  `type`: String

) extends Ingestable

case class RpcReceiptResult(  
  jsonrpc:String,
  result:Option[RpcReceipt],
  id: Any
) 

case class RpcBlockReceiptsResult(
  jsonrpc:String,  
  result:Array[RpcReceipt],
  id: Any
)