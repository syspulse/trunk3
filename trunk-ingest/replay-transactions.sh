#!/bin/bash
CWD=`echo $(dirname $(readlink -f $0))`
TX=${1}

ETH_RPC_URL=${ETH_RPC_URL:-http://geth:8545}

>&2 echo $TX

shift

$CWD/run-trunk.sh replay \
   -e tx.extractor \
   -f $ETH_RPC_URL \
   --block=rpc \
   "--filter=$TX" \
   --batch=1 \
   --block.throttle=1000 \
   --format=json \
   $@

