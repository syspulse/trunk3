# stat

Collect stat telemetry for Blockchain

## Run

### Run Stat from Kafka topic
```
./run-stat.sh -f kafka://broker-1:9092/ethereum.mainnet.tx -e tx.extractor
```

### Run Stat from Websocket Proxy:

```
./run-stat.sh -f ws://proxy -e tx.extractor
```

### Run Stat from multiple feeds, use Redis namespace '0' and show stat only for the second feed (arb)

```
./run-stat.sh -e tx.extractor --datastore=redis://host:6371/0 -f ws://eth-proxy,kafka://broker-1:9092/arb.mainnet.tx -o null://,stdout://
```
