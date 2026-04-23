BLOCK=${1:-408538997}
ENCODING=${2:-json}
TRANSACTION_DETAILS=${3:-full}
REWARDS=${4:-true}
MAX_TX_VERSION=${5:-0}

curl https://api.mainnet-beta.solana.com \
  -X POST \
  -H "Content-Type: application/json" \
  -d @- <<EOF
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "getBlock",
  "params": [
    $BLOCK,
    {
      "encoding": "$ENCODING",
      "transactionDetails": "$TRANSACTION_DETAILS",
      "rewards": $REWARDS,
      "maxSupportedTransactionVersion": $MAX_TX_VERSION
    }
  ]
}
EOF
