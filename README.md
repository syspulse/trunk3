# trunk3

Streaming Web3 RPC ingestion engine

- Multichain (see Sources)
- `Latest` state support (file)
- Range stream (`start`:`end` blocks)
- Entities (`block`,`transaction`,`tx`(fat transaction),`log`)
- Lag support (to avoid re-orgs)
- Reorg support (to detect re-orgs)
- Flexible Source/Sink (stdin/stdout,file,kafka)
- Support to write to Parquet files Sink
- Support to write to S3 files
- File Sink timestamp partitions support


<img src="doc/trunk3-Architecture-overview.drawio.png" width="800">


## Sources

| source | uri | description |
|--------|-----|--------------|
| EVM    | http://geth:8545 | Standard EVM RPC             |
| [ethereumetl](https://github.com/syspulse/ethereum-etl)|     | From ethereumetl stream (kafka) |
| ICP | icp:// | Dfinity Rosetta/[Ledger](https://ledger-api.internetcomputer.org/swagger-ui/#/) RPC | 
| Starknet | stark:// | Starknet RPC (default is public) |
| Vechain | vechain:// | Vechain RPC | 
|     |  |

By default Source is `stdin`

## Entities

Entity is specified as `entity.blockchain` (e.g `block.icp` - ICP block)

| Entity |  Supported Blockchain| description |
|--------|-----|--------------|
| block       | .eth .icp .stark| Block       |
| transaction | .eth .icp .stark| Transaction (with status and block info)     |
| log         | .eth| Event Logs | 
| token       | .eth| Token Transfer |
| tx          | .eth .stark| Fat Transaction (with block, receupt and logs ) | 
|     |  |


If no blockchain suffix is specified, `eth` (`rpc`) is assumed
 
## Sinks

`trunk3` uses [skel-ingest](https://github.com/syspulse/skel/tree/main/skel-ingest/ingest-flow) Pipeline engine and can stream into any supported sinks:

https://github.com/syspulse/skel/tree/main/skel-ingest/ingest-flow#output-feeds


By default it streams into `stdout` without formatting


## Usage Examples

Run from [trunk-ingest](trunk-ingest) directory

### via RPC

Blocks from latest:
```
./run-trunk.sh -e block -f http://geth:8545 --block=latest 
```

Blocks catch-up:
```
./run-trunk.sh -e block -f http://geth:8545 --throttle=30000
```

Blocks from specific block in batches:
```
./run-trunk.sh -e block -f http://geth:8545 --block=19999 --batch=10
```

Blocks Range:
```
./run-trunk.sh -e block -f http://geth:8545 --block=0 --block.end=100
```

Blocks from the state file (to continue stream with restarts)
```
./run-trunk.sh -e block -f http://geth:8545 --block=file://BLOCKS 
```

Transactions + Logs:
```
./run-trunk.sh -e tx -f http://geth:8545
```

### Lag (to prevent reorg-ed data)

It will return blocks in past from lastest block at the depth of `lag` parameter

```
./run-trunk.sh -e block.eth -f http://geth:8545 --delimiter= --block=latest --lag=2 
```

### Reorg Detection (Blockchain re-organizations)

`--reorg` option allows to catch reorganizations when new block replaces old one. `reorg` specifies how deep the memory for old blocks must be to detect reorg

__NOTES__: 
1. Important to have `throttle` small enough to detect fast reorgs (more detection than etherscan)
2. `--lag` and `--reorg` are not compatible and should not be used together

```
./run-trunk.sh -e block -f http://geth:8545 --delimiter= --block=latest --logging=WARN --reorg=2 --throttle=1000
```

----

## ICP

From default Ledger API

```
./run-trunk.sh -f icp:// -e transaction.icp
```

----

## VeChain

From default VeChain RPC

```
./run-trunk.sh -f vechain:// -e transaction.vechain
```

----

## Starknet

From Infura Starknet RPC

Export Infura API key:

```
export INFURA_KEY=1234
```

```
./run-trunk.sh -f stark:// -e transaction.stark --api.token=$INFURA_KEY
```

----