CWD=`echo $(dirname $(readlink -f $0))`

BLOCK=${1}

echo $BLOCK >/tmp/BLOCK

shift

ETH_RPC_URL=${ETH_RPC_URL:-http://geth:8545}

$CWD/run-trunk.sh replay \
   -e tx.extractor \
   -f $ETH_RPC_URL \
   --block=list:///tmp/BLOCK \
   --batch=1 \
   --block.throttle=1000 \
   --format=json \
   $@

