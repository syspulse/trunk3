# Solana RPC 

Explorer: https://explorer.solana.com/

RPC Spec: https://solana.com/rpc

RPC (mainnet beta): https://api.mainnet-beta.solana.com
RPC (devnet): https://api.devnet.solana.com
RPC (Testnet): https://api.testnet.solana.com

## Providers

- https://www.helius.dev/


__WIP__

Currently supported:

- Blocks
- Transactions

## Usage

### Ingest blocks from public devnet

Since public RPC has rate limit, we need to specify batch limit to avoid overloading RPC with batches

```
./run-trunk.sh -f sol:dev:// -e block.solana --batch=1 --block.throttle=1000 --throttle=3000
```

### Ingest from free Helius

Free subscription does not support RPC batches

`--batch=1` - no batching in Pipeline
`--block.limit=1` - no request batching to RPC

```
./run-trunk.sh -e transaction.solana -f "https://mainnet.helius-rpc.com/?api-key=$HELIUS_API_KEY" --block.limit=1 --batch=1 --throttle=10000
```