BLOCK_FILE=${1}
TX_FILE=${2}

./run-trunk.sh -e tx.extractor \
   -f http://geth:8545 \
   --block=list://${BLOCK_FILE} \
   --filter=file://${TX_FILE} \
   --batch=1 \
   --block.throttle=1000 \
   --output=json \
   $@

