CWD=`echo $(dirname $(readlink -f $0))`
BLOCK=${1}
TX=${2}

ETH_RPC_URL=${ETH_RPC_URL:-http://geth:8545}

echo $BLOCK >/tmp/BLOCK
echo $TX >/tmp/TX

shift
shift

$CWD/run-trunk.sh replay \
   -e tx.extractor \
   -f $ETH_RPC_URL \
   --block=list:///tmp/BLOCK \
   --filter=file:///tmp/TX \
   --batch=1 \
   --block.throttle=1000 \
   --format=json \
   $@

