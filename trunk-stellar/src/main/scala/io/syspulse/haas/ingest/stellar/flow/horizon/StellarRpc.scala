package io.syspulse.haas.ingest.stellar.flow.horizon

import com.typesafe.scalalogging.Logger

import io.syspulse.skel.Ingestable

// Root PRC
// {
//   "_links": {
//     ...
//   },
//   "horizon_version": "2.28.0-93f9d706abadbe1594544093a7065665d26bc5cc",
//   "core_version": "stellar-core 20.2.0.rc2 (368063acf47ddb85969dd6e5b5cc8d48c8b1342b)",
//   "ingest_latest_ledger": 392549,
//   "history_latest_ledger": 392549,
//   "history_latest_ledger_closed_at": "2024-02-04T14:25:24Z",
//   "history_elder_ledger": 2,
//   "core_latest_ledger": 392549,
//   "network_passphrase": "Test SDF Network ; September 2015",
//   "current_protocol_version": 20,
//   "supported_protocol_version": 20,
//   "core_supported_protocol_version": 20
// }

case class StellarRpcRoot(
  horizon_version: String,
  core_version: String,
  ingest_latest_ledger: Long,
  history_latest_ledger: Long,
  history_latest_ledger_closed_at: String,
  history_elder_ledger: Int,
  core_latest_ledger: Long,
  network_passphrase: String,
  current_protocol_version: Int,
  supported_protocol_version: Int,
  core_supported_protocol_version: Int
)


//
// {
//   "_links": {
//     ...
//   },
//   "id": "c19031bc304d8743f2b54aefcbc77deb3736c5bc5a4118f350edbf3ecff12c98",
//   "paging_token": "1685985117077504",
//   "hash": "c19031bc304d8743f2b54aefcbc77deb3736c5bc5a4118f350edbf3ecff12c98",
//   "prev_hash": "059e51bb33ac6b1d3905ae06511fd0580244936e2afefecf98924efe5ecbbecf",
//   "sequence": 392549,
//   "successful_transaction_count": 1,
//   "failed_transaction_count": 3,
//   "operation_count": 1,
//   "tx_set_operation_count": 5,
//   "closed_at": "2024-02-04T14:25:24Z",
//   "total_coins": "100000000000.0000000",
//   "fee_pool": "24046.2965713",
//   "base_fee_in_stroops": 100,
//   "base_reserve_in_stroops": 5000000,
//   "max_tx_set_size": 105,
//   "protocol_version": 20,
//   "header_xdr": "AAAAFAWeUbszrGsdOQWuBlEf0FgCRJNuKv7+z5iSTv5ey77P60XHBa1phOqRclESj01cq5+l0/KnK5XOqVDUDeeRnrUAAAAAZb+eVAAAAAAAAAABAAAAAKgkzRi8nXUGTSmaW1uspDvDqi8yaTgVPYwvm7XLbfAzAAAAQF5nEmhz8SM9K9yev2U4RoWv51YSPMLp4nKnigrtksH4gcD3V5M9Thy6sbfb2EZEtqGbjhIHRNl+zRv2zmO+XA7/40zPXpIk1nqwvJhnUe8GYABTmqJH7aWKvSQYPaxXbinfD0efGWknzMrp5eNWIm7KyiqJUe4GeKIxj9wZwX80AAX9ZQ3gtrOnZAAAAAAAN/y1q9EAAAAAAAAAAAAAKGIAAABkAExLQAAAAGnQABd1ZgR8DQLGMOJlxGCGrGfg+E91pIXMf4bOEGUZq7tI3Ggpi5dPfmJANBoF6AmnUJI/4hobFiiq+B46cwYQYJqssuKTi7CSb3LcBb40cGEK4sX+AGwFEnnlV6POyTgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
// }
case class StellarRpcBlock(
  id: String,
  paging_token: String,
  hash: String,
  prev_hash: String,
  sequence: Long,
  successful_transaction_count: Int,
  failed_transaction_count: Int,
  operation_count: Int,
  tx_set_operation_count: Int,
  closed_at: String,
  total_coins: String,
  fee_pool: String,
  base_fee_in_stroops: Long,
  base_reserve_in_stroops: Long,
  max_tx_set_size: Int,
  protocol_version: Int,
  header_xdr: String
) extends Ingestable


// {
//   "_links": {
//     ...
//   },
//   "_embedded": {                                                                                                                                                                                                          
//     "records": [                                                                                                                                                                                                          
//       {
//         "_links": {
//         },
//         "id": "b272cc5985b997aa69c61b760ccf14d64033eccabda62c6a5f60dda080b94fa2",
//         "paging_token": "1685985117093888",
//         "successful": true,
//         "hash": "b272cc5985b997aa69c61b760ccf14d64033eccabda62c6a5f60dda080b94fa2",
//         "ledger": 392549,
//         "created_at": "2024-02-04T14:25:24Z",
//         "source_account": "GCLA2E3LQDPAPJLHYDMB5R65ASGLNXWGJCX4TX7XA75C7VTJ7Y2OTZXA",
//         "source_account_sequence": "16621523441985",
//         "fee_account": "GCLA2E3LQDPAPJLHYDMB5R65ASGLNXWGJCX4TX7XA75C7VTJ7Y2OTZXA",
//         "fee_charged": "10236998",
//         "max_fee": "10236998",
//         "operation_count": 1,
//         "envelope_xdr": "AAAAAgAAAACWDRNrgN4HpWfA2B",
//         "result_xdr": "AAAAAACcNEYAAAAAAAAAAQAAAAA",
//         "result_meta_xdr": "AAAAAwAAAAAAAAACAAAAAwAF/W", 
//         "fee_meta_xdr": "AAAAAgAAAAMABf0qAAA",
//         "memo_type": "none",
//         "signatures": [
//           "5rv5fUAfo+Pmwx8EiKY5+vB9BMQJ/j7fotWvrLjQMfZpEDuXImRIIbMdo+aYJu4OrYWa/0l2dPZGuOo+O22IAA=="
//         ],
//         "valid_after": "1970-01-01T00:00:00Z",
//         "preconditions": {
//           "timebounds": {
//             "min_time": "0"
//           },
//           "min_account_sequence": "0"
//         }
//       }
//     ]
//   }
// }

case class StellarRpcEmbedded(
  records: Seq[StellarRpcTransaction]
)

case class StellarRpcTransactions(
  _embedded: StellarRpcEmbedded
)

case class StellarRpcTimebounds(
  min_time: String
)

case class StellarRpcPrecondition(
  timebounds: StellarRpcTimebounds,
  min_account_sequence: Option[String]
)

case class StellarRpcTransaction(
  id: String,
  paging_token: String,
  successful: Boolean,
  hash: String,
  ledger: Long,
  created_at: String,
  source_account: String,
  source_account_sequence: String,
  fee_account: String,
  fee_charged: String,
  max_fee: String,
  operation_count: Int,
  envelope_xdr: String,
  result_xdr: String,
  result_meta_xdr: String, 
  fee_meta_xdr: String,
  memo_type: String,
  signatures: Seq[String],
  valid_after: String,
  preconditions: StellarRpcPrecondition
    
) extends Ingestable
