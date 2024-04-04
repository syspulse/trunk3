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

Since public RPC has rate limit, we need to specify 

```
./run-trunk.sh -f sol:dev:// -e block.solana --batch=1 --block.throttle=1000 --throttle=3000
```