CWD=`echo $(dirname $(readlink -f $0))`

BLOCK_FILE=${BLOCK_FILE:-/tmp/BLOCKS.log}
TX_FILE=${TX_FILE:-/tmp/TX.log}
ETH_RPC_URL=${ETH_RPC_URL:-http://geth:8545}

$CWD/run-trunk.sh replay \
   -e tx.extractor \
   -f $ETH_RPC_URL \
   --block=list://${BLOCK_FILE} \
   --filter=file://${TX_FILE} \
   --batch=1 \
   --block.throttle=2000 \
   --throttle=1000 \
   --format=json \
   "$@"
