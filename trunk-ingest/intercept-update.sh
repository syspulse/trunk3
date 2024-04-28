#!/bin/bash

ID=${1:-1}
SCRIPT_FILE=${2:-scripts/script-eth-tx2.js}

ACCESS_TOKEN=${ACCESS_TOKEN-`cat ACCESS_TOKEN`}

SERVICE_URI=${SERVICE_URI:-http://localhost:8080/api/v1/trunk}

>&2 echo "ID=$ID"
>&2 echo "SCRIPT_FILE=$SCRIPT_FILE"

SCRIPT=`jq -R -s '.' < $SCRIPT_FILE  | sed 's/\\n//g'`

>&2 echo "SCRIPT=$SCRIPT"

#exit 0

#DATA_JSON="{\"email\":\"$EMAIL\",\"name\":\"$NAME\",\"name\":\"$NAME\",\"xid\":\"$XID\"}"
# if [ "$ID" == "" ]; then
#    DATA_JSON="{\"id\":\"$ID\",\"src\":\"$SCRIPT\"}"
# else
#    DATA_JSON="{\"id\":\"1\",\"src\":\"$SCRIPT\"}"
# fi
DATA_JSON="{\"id\":\"$ID\",\"src\":$SCRIPT}"

2> echo $DATA_JSON
curl -S -s -D /dev/stderr -X PUT --data "$DATA_JSON" -H 'Content-Type: application/json' -H "Authorization: Bearer $ACCESS_TOKEN" $SERVICE_URI/${ID}
