BLOCK=${1}
TX=${2}

echo $BLOCK >/tmp/BLOCK
echo $TX >/tmp/TX

shift
shift

./run-trunk.sh replay \
   -e tx.extractor \
   -f http://geth:8545 \
   --block=list:///tmp/BLOCK \
   --filter=file:///tmp/TX \
   --batch=1 \
   --block.throttle=1000 \
   --format=json \
   $@

