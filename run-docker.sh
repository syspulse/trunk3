#!/bin/bash                                                                                                                                                                                            
CWD=`echo $(dirname $(readlink -f $0))`
#cd $CWD

t=`pwd`;
APP=`basename "$t"`
CONF=`echo $APP | awk -F"-" '{print $2}'`

export SITE=${SITE:-$CONF}

DOCKER="syspulse/${APP}:latest"

# Datastore Directory for trunk (inside docker)
DATASTORE=${DATASTORE:-/data}
# Datstore directory on host
DATA_DIR=${DATA_DIR:-./data}

if [ "$S3_BUCKET" != "" ]; then
   PRIVILEGED="--privileged"
fi

#>&2 echo "APP: $APP"
#>&2 echo "SITE: $SITE"
#>&2 echo "DOCKER: $DOCKER"
#>&2 echo "DATA_DIR: $DATA_DIR"
#>&2 echo "ARGS: $@"
#>&2 echo "OPT: $OPT"
#>&2 echo "DATASTORE: $DATASTORE"

# Try to create data directory
mkdir -p $DATA_DIR 2>/dev/null

docker run --rm --name $APP \
   -v `pwd`/conf:/app/conf \
   -v $DATA_DIR:/data \
   -e JAVA_OPTS=$OPT \
   -e DATASTORE=$DATASTORE \
   $PRIVILEGED \
   $DOCKER $@
