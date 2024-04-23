package io.syspulse.haas.ingest.eth.flow.rpc3

import com.typesafe.scalalogging.Logger

// ATTENTION
import io.syspulse.skel.service.JsonCommon
import io.syspulse.skel.Ingestable

import spray.json._
import spray.json.{DefaultJsonProtocol,NullOptions}
import io.syspulse.skel.util.Util

// --- Mempool ----
// {"jsonrpc":"2.0","method":"eth_subscription",
// "params":{"subscription":"0x295c3b8e80401f6aeb147568b8fa6d32","result":"0x6f8860f533efb4dd529ea61a1defb3ab2d4e39854a03e3354c5a427342051cda"}}
case class RpcMempoolSubscription(
  subscription:String,
  result:String
)

case class RpcWsMempoolResult(
  jsonrpc:String, 
  method: String,
  params: RpcMempoolSubscription
)

case class RpcResult(
  jsonrpc:String,  
  result: Option[String] // possible response
)


// {
//   "hash": "0xfc7d55e49b423d15634182a964a65a5583d8af34c484ea6727b8bddf6026e405",
//   "nonce": "0x124",
//   "blockHash": "0xcfbab0a009e71b1e6e6646714d90a057cf3f8f60636bb31a0d145b724f849bb2",
//   "blockNumber": "0xee3b60",
//   "transactionIndex": "0xb0",
//   "from": "0x1db47c0910456247464c92634d3d09e62d084b4f",
//   "to": "0xcbd6832ebc203e49e2b771897067fce3c58575ac",
//   "value": "0xd371f715aa7998",
//   "gasPrice": "0x15e753b8d",
//   "gas": "0x5208",
//   "input": "0x",
//   "r": "0x5330aca97f07e482d266dfbaf61b36b961dc90c447d5f3ba4e7ffe9e1bb2aa40",
//   "s": "0x2101bc9a51ccae28ccba0e78f428c9d06bea50aef65949c092735ffae884ae7a",
//   "v": "0x25",
//   "chainId": "0x1",
//   "type": "0x0"
// }

// {
//   "blockHash": "0x1415d63b704ff35e6f6056121fc8bd6aeb0672682c47409413b11720f4001252",
//   "blockNumber": "0x10b7448",
//   "from": "0x5eed8ce3cd7ade7e7ec1cd79b95f36aac01ba433",
//   "gas": "0x14f1f",
//   "gasPrice": "0x3b5af8bac",
//   "maxFeePerGas": "0x518723801",
//   "maxPriorityFeePerGas": "0x5f5e100",
//   "hash": "0x912f229938f94d89e159551e38027383f84b744e20bc3d90183d341adf456668",
//   "input": "0xa9059cbb0000000000000000000000000abb6984f174e4f5cef156c3dc2b3bb823f553160000000000000000000000000000000000000000000000001bc16d674ec80000",
//   "nonce": "0x47",
//   "to": "0x1f9840a85d5af5bf1d1762f925bdaddc4201f984",
//   "transactionIndex": "0x68",
//   "value": "0x0",
//   "type": "0x2",
//   "accessList": [],
//   "chainId": "0x1",
//   "v": "0x1",
//   "r": "0x55b0b6793407f82f821f13d53e75343513f5e0b43d248bbbfb4f4176331f2599",
//   "s": "0x12e08dd095d38474c020d34e3c29b34137426e7824a050dab42d37df2ba0ede1"
// }

case class RpcTx(
  hash:String,
  nonce:String,
  blockHash: String,
  blockNumber: String,
  transactionIndex: String,
  from: String,
  to: Option[String],
  value: String,
  gasPrice: String,
  gas: String,
  input: String,
  r: Option[String], // zkSync may not have signature
  s: Option[String],
  v: Option[String],
  chainId: Option[String],
  `type`: Option[String],

  maxFeePerGas: Option[String] = None,
  maxPriorityFeePerGas: Option[String] = None,
  //accessList:Option[List[String]] = None,

  timestamp:Option[Long] = None // NOT FROM RPC !!! used internally for streaming Block timestamp 
)  extends Ingestable


case class RpcUncle(
  hash:String
)

// {                                                                                                                                                                                                                                                          
//  "jsonrpc": "2.0", 
//  "result": {                                                                                                                                                                                                                                              
//     "hash": "0x1415d63b704ff35e6f6056121fc8bd6aeb0672682c47409413b11720f4001252",                                                                                                                                                                          
//     "parentHash": "0x01645ffc50c0288d4c388b89e6cb2292da0d329cd8489c9a5784c71849fe6f90",                                                                                                                                                                    
//     "sha3Uncles": "0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347",                                                                                                                                                                    
//     "miner": "0x284e210169f49625faa61b044bee3b840850f232",                                                                                                                                                                                                 
//     "stateRoot": "0x6976fd09ac0ca9968d5df26ce4b630212e365cee3b82a55bff2518fe7347b8c1",                                                                                                                                                                     
//     "transactionsRoot": "0x49cdc211dac48f4a0da8b4c06eeaf560af531d6a43e070ab748bdcbf95a361cf",                                                                                                                                                              
//     "receiptsRoot": "0xe90c5f25266841bcfaac7450b3d7374538d7eff29d53254ac872cc4c867d9a62",                                                                                                                                                                  
//     "logsBloom": "0x452f81355122407e510c4500b230392910c10545ac859165ec8110c0beb120881302918298e4e5c494c013013030a194270108e9bd2339caf0454ee862bab23218fec55e415869aaeb23d84d3e2800fe605dafc845f41eb120443fd2cd630434bbd40c42fa0ac1872f02009ec53c3c5100d841b
// 10c00a474634351f7000a39120f71d7f841940d8d2458704013125264d4cb3fa1dd41c9a8fe37c8f3e0b50e71dfaa31d1986ee4d17ab2c4e69c261fa0c60452209087589e204e6e5e600c0270af25b3277e20ce486849094b443cd0ac26686fce56e80e5f438841ea96103b4554b2e06c9b08f0884e05f18421e0384481
// 28f97a230c7152d90d5c7609ca3465",                                                                                                                                                                                                                           
//     "difficulty": "0x0",                                                                                                                                                                                                                                   
//     "number": "0x10b7448",                                                                                                                                                                                                                                 
//     "gasLimit": "0x1c9c380",                                                                                                                                                                                                                               
//     "gasUsed": "0xa74103",                                                                                                                                                                                                                                 
//     "timestamp": "0x6492e57f",                                                                                                                                                                                                                             
//     "extraData": "0xd883010c00846765746888676f312e32302e33856c696e7578",                                                                                                                                                                                   
//     "mixHash": "0x5f205a95682fefeb319a8fe794aec34bc7d65d088d38f7ff1f5ea408c1935bb9",                                                                                                                                                                       
//     "nonce": "0x0000000000000000",                                                                                                                                                                                                                         
//     "baseFeePerGas": "0x3afb9aaac",                                                                                                                                                                                                                        
//     "withdrawalsRoot": "0x584c84ca07bcef86e16b390f70daa6d8c4d9451b0c187948675b443a74a1e4c5",                                                                                                                                                               
//     "totalDifficulty": "0xc70d815d562d3cfa955",                                                                                                                                                                                                            
//     "uncles": [],                                                                                                                                                                                                                                          
//     "transactions": [ 
//     ]
//         "size": "0x12c16"
//   },
//   "id": 1
// }
case class RpcBlockResult(  
  hash:String,  
  parentHash:String,
  sha3Uncles:String,
  miner:String,
  stateRoot:String,
  transactionsRoot:String,
  receiptsRoot:String,
  logsBloom:String,
  difficulty:String,
  number:String,
  gasLimit:String,
  gasUsed:String,
  timestamp:String,
  extraData:String,
  mixHash:String,
  nonce:String,
  baseFeePerGas:Option[String],
  withdrawalsRoot: Option[String],
  totalDifficulty: String,
  
  //uncles: Seq[RpcUncle],
  uncles: Array[String],

  transactions: Array[RpcTx],
  size:String,  
    
)  extends Ingestable


case class RpcBlock(  
  jsonrpc:String,  
  result:Option[RpcBlockResult],
  id: Any
)  extends Ingestable


case class RpcLog(
  address:String,
  topics:Array[String],
  data:String,
  blockNumber:String,
  transactionHash:String,
  transactionIndex:String,
  blockHash:String,
  logIndex:String,
  removed:Boolean
)


case class RpcReceipt(
  blockHash: String,
  blockNumber: String,
  contractAddress: Option[String],
  cumulativeGasUsed: String,
  effectiveGasPrice: Option[String],
  from: String,
  gasUsed: String,
  logs: Array[RpcLog],

  logsBloom: String,
  status: Option[String],

  to: Option[String],
  transactionHash: String,
  transactionIndex: String,
  `type`: Option[String],

  timestamp:Option[Long] = None // NOT FROM RPC !!! used internally for streaming Block timestamp 
)

case class RpcReceiptResultBatch(  
  jsonrpc:String,  
  result:Option[RpcReceipt],
  id: Any
) extends Ingestable

case class RpcBlockReceiptsResult(  
  jsonrpc:String,  
  result:Array[RpcReceipt],
  id: Any
) extends Ingestable

case class RpcTokenTransfer(data:String)


// --- Mempool ---------------------------------------------------------------------------------------------------------------------------------------
case class RpcAccessList(address:String,storageKeys:Array[String])
// case class RpcTxMempool(
//   ts:Long,
//   pool:String, // queued, pending
//   blockHash:Option[String],
//   blockNumber:Option[Long],
//   from: String,
//   gas: Long,
//   gasPrice: BigInt,
//   maxFeePerGas: Option[BigInt],
//   maxPriorityFeePerGas: Option[BigInt],
//   hash: String,
//   input: String,
//   nonce: Long,
//   to: Option[String],
//   transactionIndex: Option[Int],
//   value: BigInt,
//   `type`: Byte,
//   accessList: Option[Array[RpcAccessList]],
//   chainId: Option[Int],
//   v: Byte,
//   r: String,
//   s: String
// ) extends Ingestable 

case class RpcTxRaw(
  blockHash:Option[String],
  blockNumber:Option[Long],
  from: String, 
  gas: String,
  gasPrice: String,
  maxFeePerGas: Option[String],
  maxPriorityFeePerGas: Option[String],
  hash: String,
  input: String,
  nonce: String,
  to: Option[String],
  transactionIndex: Option[Int],
  value: String,
  `type`: String,
  accessList: Option[Array[RpcAccessList]],
  chainId: Option[String],
  v: Option[String],
  r: Option[String], // zkSync may not have signature
  s: Option[String],  

) {
  
  // def unraw(ts:Long,pool:String):RpcTxMempool = RpcTxMempool(
  //     ts,
  //     pool,
  //     this.blockHash,
  //     this.blockNumber,
  //     this.from: String,
  //     java.lang.Long.parseLong(this.gas.drop(2),16),
  //     BigInt(Util.unhex(gasPrice)),
  //     maxFeePerGas.map(v => BigInt(Util.unhex(v))),
  //     maxPriorityFeePerGas.map(v => BigInt(Util.unhex(v))),
  //     this.hash,
  //     this.input,
  //     Integer.parseInt(nonce.drop(2),16),
  //     this.to,
  //     this.transactionIndex,
  //     BigInt(Util.unhex(value)),
  //     Integer.parseInt(`type`.drop(2),16).toByte,
  //     this.accessList,
  //     chainId.map(v => Integer.parseInt(v.drop(2),16)),
  //     Integer.parseInt(v.drop(2),16).toByte,
  //     this.r,
  //     this.s
  // )
}

case class RpcTxPool(pending:Map[String,Map[String,RpcTxRaw]],queued:Map[String,Map[String,RpcTxRaw]])

case class RpcTxPoolResult(
  jsonrpc:String,
  id:Long,
  result:RpcTxPool
)

case class RpcMempoolTransaction (
  blockHash:Option[String],
  blockNumber:Option[String],
  from:String,
  gas:String,
  gasPrice:String,
  maxFeePerGas:Option[String],
  maxPriorityFeePerGas:Option[String],
  hash:String,
  input:Option[String],
  nonce:String,
  to:Option[String],
  transactionIndex:Option[String],
  value:Option[String],
  `type`:Option[String],
  accessList: Option[Array[RpcAccessList]],
  chainId:Option[String],
  v:String,
  r:String,
  s:String,
  yParity:Option[String]
)

case class RpcMempoolTransactionResult(
  jsonrpc:String,
  id:Long,
  result:Option[RpcMempoolTransaction]
)

object EthRpcJson extends JsonCommon {
  
  implicit val jf_rpc_tx_al = jsonFormat2(RpcAccessList)
  // implicit val jf_rpc_tx_mem = jsonFormat19(RpcTxMempool)
  implicit val jf_rpc_tx_raw = jsonFormat19(RpcTxRaw)
  implicit val jf_rpc_mem = jsonFormat2(RpcTxPool)
  implicit val jf_rpc_mem_res = jsonFormat3(RpcTxPoolResult)

  implicit val jf_rpc_tx = jsonFormat19(RpcTx)
  implicit val jf_rpc_uncle = jsonFormat1(RpcUncle)
  implicit val jf_rpc_res = jsonFormat22(RpcBlockResult)
  implicit val jf_rpc_bl = jsonFormat3(RpcBlock)

  implicit val jf_rpc_tt = jsonFormat1(RpcTokenTransfer)
  implicit val jf_rpc_log = jsonFormat9(RpcLog)
  implicit val jf_rpc_rec = jsonFormat15(RpcReceipt)  
  implicit val jf_rpc_rec_res = jsonFormat3(RpcReceiptResultBatch)  
  implicit val jf_rpc_blk_rec = jsonFormat3(RpcBlockReceiptsResult)  

  // websocket
  implicit val jf_rpc_ws_mem_sub = jsonFormat2(RpcMempoolSubscription)  
  implicit val jf_rpc_ws_mem = jsonFormat3(RpcWsMempoolResult)  
  implicit val jf_rpc_ws_res = jsonFormat2(RpcResult)
  
  implicit val jf_rpc_mem_tx = jsonFormat20(RpcMempoolTransaction)  
  implicit val jf_rpc_mem_tx_res = jsonFormat3(RpcMempoolTransactionResult)  
}
