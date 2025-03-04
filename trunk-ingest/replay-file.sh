BLOCK_FILE=${1}
TX_FILE=${2}

shift
shift

./run-trunk.sh replay \
   -e tx.extractor \
   -f http://geth:8545 \
   --block=list://${BLOCK_FILE} \
   --filter=file://${TX_FILE} \
   --batch=1 \
   --block.throttle=250 \
   --format=json \
   $@

