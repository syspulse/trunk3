JSON_PARSED=${1}
TX=${2}

cat ${JSON_PARSED} | jq -c .result.transactions[] | grep $TX | jq .


