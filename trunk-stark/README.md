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

Default Lava Node (https://www.lavanet.xyz/get-started/starknet) does not support batch requests, 
so either use Infura (`stark://infura` or directly https://infura) or enforce `--batch=1` (and slow down for RPC request between blocks):

```
./run-trunk.sh -f stark:// -e transaction.stark --batch=1 --block.throttle=250
```
