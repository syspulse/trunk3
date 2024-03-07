# Ethereum RPC 

Standard Ethereum RPC

## Run

### Run Tx ingest with ETL compatibility:

```
./run-trunk.sh -f http://geth:8545 -e tx.extractor -o json://
```

### Running against local `hardhat`/`anvil` in testing mode

It is important to specify `--receipt.request=batch` since anvil does not support block receipts

```
docker run --rm -p 9300:9300 syspulse/trunk3 -f http://172.17.0.1:8545 -e tx.extractor -o ws:server://0.0.0.0:9300 --format=json --receipt.request=batch
```
