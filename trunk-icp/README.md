# ICP RPC

Supports old `Rosetta` and new `Ledger` RPC

## Notes

- Since Dfinity does not have a concept of a Block, `trunk3` produces Blocks with a single Transaction only for *compatability* with other entities.
- ICP does not have a concept of contract calls transactions, so RPC does not provide this information

### Usage example

```
./run-trunk.sh -f icp:// -e transaction.icp
```

