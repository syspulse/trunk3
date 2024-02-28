package io.syspulse.haas.ingest.icp.flow.ledger

import com.typesafe.scalalogging.Logger

import io.syspulse.skel.Ingestable

// /transactions
// {                                                                                                         
//   "total": 9333372,                                                                                       
//   "blocks": [                                                                                             
//     {                         
//       "block_height": "9333371", 
//       "parent_hash": "bf0c72e06bd602150136546fa3a035829aac571cffaa33bbf5a99b855dcc2615",
//       "block_hash": "6d7bf20b74340fec938a4c96507fcd09d5ef32cf02bc36037e9e82afd3159735",
//       "transaction_hash": "4a5352252c68d6b0d3403dd6ff3d22991580b0921b23927f0e9871b0a22df0a8",
//       "from_account_identifier": "efe871c8cf4d2cc8b7b750d16fb3767028181853ef9a06fde1f79e6a62b6a273",
//       "to_account_identifier": null,
//       "spender_account_identifier": "0dd5baaf2a03c6b44859c881ef92c17de0b063db5a202603eb790256772d3d75",
//       "transfer_type": "approve",                                                                         
//       "amount": "18446744073709551615",                                                                   
//       "fee": "10000",                                                                                     
//       "memo": "0",                                                                                        
//       "created_at": 1703597808,                       
//       "allowance": "18446744073709551615",                                                                
//       "expected_allowance": null,                                                                         
//       "expires_at": null,                                                                                 
//       "icrc1_memo": null                                                                                  
//     }                                                                                                     
//   ]                           
// } 


// Ledger API Spec
// https://ledger-api.internetcomputer.org/swagger-ui/
// 
// https://ic-api.internetcomputer.org/api/v3/swagger
// https://ic-api.internetcomputer.org/api
// brief: https://mmapped.blog/posts/13-icp-ledger

case class IcpRpcBlock(
  block_height: String, 
  parent_hash: String,
  block_hash: String,
  transaction_hash: String,
  from_account_identifier: Option[String],
  to_account_identifier: Option[String],
  spender_account_identifier: Option[String],
  transfer_type: String,
  amount: String,
  fee: String,
  memo: String,
  created_at: Long,
  allowance: Option[String],
  expected_allowance: Option[String],
  expires_at: Option[Long],
  icrc1_memo: Option[String],
) extends Ingestable

case class IcpRpcTransactions(
  total: Long,
  blocks:Seq[IcpRpcBlock],
    
) extends Ingestable
