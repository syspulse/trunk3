package io.syspulse.haas.ingest.icp.flow.rosetta

import com.typesafe.scalalogging.Logger

import io.syspulse.skel.Ingestable

// {
//   "block": {
//     "block_identifier": {
//       "index": 7305956,
//       "hash": "9e066d9f7a5751384a42b1ba8b867db6bdf756c1cccf2cc2bc5ce2828b98b6eb"
//     },
//     "parent_block_identifier": {
//       "index": 7305955,
//       "hash": "c65e002051838a109fb909b8ca5ebd188d13fa3c2f7f11986f8cdacb6efbd67d"
//     },
//     "timestamp": 1701271554039,
//     "transactions": [
//       {
//         "transaction_identifier": {
//           "hash": "4f9b3f3f26216d26bf4e82cdd422317f1ffeaefaa3c58f8b7a902afd0e9697a0"
//         },
//         "operations": [
//           {
//             "operation_identifier": {
//               "index": 0
//             },
//             "type": "TRANSACTION",
//             "status": "COMPLETED",
//             "account": {
//               "address": "c01e0d829f8bf50c958fb3dd3dc446d7dcb5efb3f18edad7fc28f3797b58ca26"
//             },
//             "amount": {
//               "value": "-1172550830",
//               "currency": {
//                 "symbol": "ICP",
//                 "decimals": 8
//               }
//             }
//           },
//           {
//             "operation_identifier": {
//               "index": 1
//             },
//             "type": "TRANSACTION",
//             "status": "COMPLETED",
//             "account": {
//               "address": "6b1eda99402172c967bf05f7f77a013a60fedbb635c4c09502fed5abd514a99b"
//             },
//             "amount": {
//               "value": "1172550830",
//               "currency": {
//                 "symbol": "ICP",
//                 "decimals": 8
//               }
//             }
//           },
//           {
//             "operation_identifier": {
//               "index": 2
//             },
//             "type": "FEE",
//             "status": "COMPLETED",
//             "account": {
//               "address": "c01e0d829f8bf50c958fb3dd3dc446d7dcb5efb3f18edad7fc28f3797b58ca26"
//             },
//             "amount": {
//               "value": "-10000",
//               "currency": {
//                 "symbol": "ICP",
//                 "decimals": 8
//               }
//             }
//           }
//         ],
//         "metadata": {
//           "block_height": 7305956,
//           "memo": 10545568565151939000,
//           "timestamp": 1701271554039099400                          
//         }
//       }
//     ]
//   }
// }

// {
//     "block": {
//         "block_identifier": {
//             "index": 1123941,
//             "hash": "0x1f2cc6c5027d2f201a5453ad1119574d2aed23a392654742ac3c78783c071f85"
//         },
//         "parent_block_identifier": {
//             "index": 1123941,
//             "hash": "0x1f2cc6c5027d2f201a5453ad1119574d2aed23a392654742ac3c78783c071f85"
//         },
//         "timestamp": 1582833600000,
//         "transactions": [
//             {
//                 "transaction_identifier": {
//                     "hash": "0x2f23fd8cca835af21f3ac375bac601f97ead75f2e79143bdf71fe2c4be043e8f"
//                 },
//                 "operations": [
//                     {
//                         "operation_identifier": {
//                             "index": 5,
//                             "network_index": 0
//                         },
//                         "related_operations": [
//                             {
//                                 "index": 1
//                             },
//                             {
//                                 "index": 2
//                             }
//                         ],
//                         "type": "Transfer",
//                         "status": "Reverted",
//                         "account": {
//                             "address": "0x3a065000ab4183c6bf581dc1e55a605455fc6d61",
//                             "sub_account": {
//                                 "address": "0x6b175474e89094c44da98b954eedeac495271d0f",
//                                 "metadata": {}
//                             },
//                             "metadata": {}
//                         },
//                         "amount": {
//                             "value": "1238089899992",
//                             "currency": {
//                                 "symbol": "BTC",
//                                 "decimals": 8,
//                                 "metadata": {
//                                     "Issuer": "Satoshi"
//                                 }
//                             },
//                             "metadata": {}
//                         },
//                         "coin_change": {
//                             "coin_identifier": {
//                                 "identifier": "0x2f23fd8cca835af21f3ac375bac601f97ead75f2e79143bdf71fe2c4be043e8f:1"
//                             },
//                             "coin_action": "coin_created"
//                         },
//                         "metadata": {
//                             "asm": "304502201fd8abb11443f8b1b9a04e0495e0543d05611473a790c8939f089d073f90509a022100f4677825136605d732e2126d09a2d38c20c75946cd9fc239c0497e84c634e3dd01 03301a8259a12e35694cc22ebc45fee635f4993064190f6ce96e7fb19a03bb6be2",
//                             "hex": "48304502201fd8abb11443f8b1b9a04e0495e0543d05611473a790c8939f089d073f90509a022100f4677825136605d732e2126d09a2d38c20c75946cd9fc239c0497e84c634e3dd012103301a8259a12e35694cc22ebc45fee635f4993064190f6ce96e7fb19a03bb6be2"
//                         }
//                     }
//                 ],
//                 "related_transactions": [
//                     {
//                         "network_identifier": {
//                             "blockchain": "bitcoin",
//                             "network": "mainnet",
//                             "sub_network_identifier": {
//                                 "network": "shard 1",
//                                 "metadata": {
//                                     "producer": "0x52bc44d5378309ee2abf1539bf71de1b7d7be3b5"
//                                 }
//                             }
//                         },
//                         "transaction_identifier": {
//                             "hash": "0x2f23fd8cca835af21f3ac375bac601f97ead75f2e79143bdf71fe2c4be043e8f"
//                         },
//                         "direction": "forward"
//                     }
//                 ],
//                 "metadata": {
//                     "size": 12378,
//                     "lockTime": 1582272577
//                 }
//             }
//         ],
//         "metadata": {
//             "transactions_root": "0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347",
//             "difficulty": "123891724987128947"
//         }
//     },
//     "other_transactions": [
//         {
//             "hash": "0x2f23fd8cca835af21f3ac375bac601f97ead75f2e79143bdf71fe2c4be043e8f"
//         }
//     ]
// }

case class IcpRpcBlockIdentifier(
  index:Long,
  hash:String,    
)

case class IcpRpcTransactionIdentifier(
  hash:String,
)

case class IcpRpcTransactionOperationIdentifier(
  index:Long,
)

case class IcpRpcAccount(
  address:String,
)

case class IcpRpcCurrency(
  symbol:String,
  decimals:Int
)

case class IcpRpcAmount(
  value:String,
  currency:IcpRpcCurrency
)

case class IcpRpcMetadata(
  block_height:Long,
  memo:BigInt,
  timestamp:Long
)

case class IcpRpcTransactionOperation(
  operation_identifier:IcpRpcTransactionOperationIdentifier,
  `type`:String,
  status:String,
  account: IcpRpcAccount,
  amount: IcpRpcAmount
)

case class IcpRpcTransaction(
  transaction_identifier:IcpRpcTransactionIdentifier,
  operations:Seq[IcpRpcTransactionOperation],
  metadata: IcpRpcMetadata,
) extends Ingestable

case class IcpRpcBlockBlock(
  block_identifier: IcpRpcBlockIdentifier,
  parent_block_identifier: IcpRpcBlockIdentifier,
  timestamp: Long,
  transactions:Seq[IcpRpcTransaction]
  
)  extends Ingestable

case class IcpRpcBlock(block:IcpRpcBlockBlock) extends Ingestable
