# Starknet RPC 

RPC Spec: https://github.com/starkware-libs/starknet-specs?tab=readme-ov-file
Playground: https://playground.open-rpc.org/?uiSchema

__WIP__

Currently supported:

- Blocks
- Transactions
- Tx (Fat transactions)

__NOTE__: Tx relies on `--receipt.request` to request receipts methods

Nodes prior to `0.7` support only `--receipt.request=batch`, new Starknet nodes support default `--receipt.request=block`

