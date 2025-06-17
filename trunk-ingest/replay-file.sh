CWD=`echo $(dirname $(readlink -f $0))`

BLOCK_FILE=${1}
TX_FILE=${2}
ETH_RPC_URL=${ETH_RPC_URL:-http://geth:8545}

shift
shift

$CWD/run-trunk.sh replay \
   -e tx.extractor \
   -f $ETH_RPC_URL \
   --block=list://${BLOCK_FILE} \
   --filter=file://${TX_FILE} \
   --batch=1 \
   --block.throttle=250 \
   --format=json \
   $@

