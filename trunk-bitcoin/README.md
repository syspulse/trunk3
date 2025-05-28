# Bitcoin Ingest

## Blocks

https://blockstream.info/block/000000000000000000011fb3678049d270d9f8bf01ea510a87d002ab4e42d964 - 5 tx

https://blockstream.info/block/000000000000000000000f572ca65a466c88f7ddb70d76befe3231a98ee1cefa - 1 tx
https://blockstream.info/block/000000000000000000023d80ea035a2cbf50406df889edb970df92ad2644e628 - 1 tx

https://blockstream.info/block/00000000000000000001125bf96de7b188365ed5b1fcfc8ebc23cf8499c1ca2e - 38 tx

## Run

__ATTENTION__: 
- Bitcoin blocks are very large (1.5M), buffer must be large enough to parse full block: `--buffer=104857600`
- Bitcoin blocks are 10 min freq: `--throttle=600000`

### Read all transactions from file:

```
./run-trunk.sh -e tx.btc -f ../trunk-bitcoin/00000000000000000001af8982cb069952d6fb494ab26daf3f6c8c90d2501d29-3.json --buffer=104857600 --format=json | jq .
```

### Read specific block from RPC

```
./run-trunk.sh -e block.btc -f bitcoin:// --throttle=600000 --buffer=104857600 --block=893268 --block.end=893268 --format=json
```
