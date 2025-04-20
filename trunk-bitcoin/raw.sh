TXID=$1

curl -X GET \
   https://blockchain.info/rawtx/${TXID}?format=json


