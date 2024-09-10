# VeChain RPC 

Currently supported

- Blocks
- Transactions

## VeChain API

Endpoint: https://mainnetc2.vechain.network

This is the default ingestion method and produces VeChain specific output format

## VeChain Energy RPC

Endpoint: https://rpc-mainnet.vechain.energy

This is EVM compatible RPC and can be used with any EVM based tools like Truffle, Web3j, OpenZeppelin, etc.

__NOTE__: It does not support batches, so it must be used like this:

```
./run-trunk.sh -f https://rpc-mainnet.vechain.energy --receipt.request=block -e tx --batch=1
```
