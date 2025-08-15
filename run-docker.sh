#!/bin/bash                                                                                                                                                                                            
#CWD=`echo $(dirname $(readlink -f $0))`
#cd $CWD

t=`pwd`;
if [ "$APP" == "" ]; then
   APP_NAME=`basename "$t"`
   APP="syspulse/${APP_NAME}"
else   
   APP_NAME=`basename "$APP"`
fi

if [ "$VERSION" == "" ]; then
   VERSION="latest"
fi

if [ "$CONF" == "" ]; then
   CONF=`echo $APP | awk -F"-" '{print $2}'`
fi

export SITE=${SITE:-$CONF}

DOCKER="${APP}:${VERSION}"

DATA_DIR=${DATA_DIR:-/tmp/data}
mkdir -p $DATA_DIR

if [ "$S3_BUCKET" != "" ]; then
   PRIVILEGED="--privileged"
fi

if [ -v MOUNT ]; then
   #
   MOUNT_OPTS=$MOUNT
else
   if [ -e `pwd`/conf ]; then
      MOUNT_OPTS="-v `pwd`/conf:/app/conf"
   fi
   MOUNT_OPTS="$MOUNT_OPTS -v $DATA_DIR:/data"
fi

>&2 echo "APP: $APP"
>&2 echo "VERSION: $VERSION"
>&2 echo "SITE: $SITE"
>&2 echo "DOCKER: $DOCKER"
>&2 echo "DATA_DIR: $DATA_DIR"
>&2 echo "ARGS: $@"
>&2 echo "OPT: $OPT"
>&2 echo "DATASTORE: $DATASTORE"
>&2 echo "DNS: $DNS"
>&2 echo "DOCKER_OPTS: $DOCKER_OPTS"
>&2 echo "MOUNT_OPTS: $MOUNT_OPTS"

if [ "$DNS" != "" ]; then
   DNS_OPT="--dns $DNS"
fi

# ATTENTION: /app/bin/skel-app overrides arguments !
#
# process_args () {
#   local no_more_snp_opts=0
#   while [ $# -gt 0 ]; do
#     case "$1" in
#              --) shift && no_more_snp_opts=1 && break ;;
#        -h|-help) usage; exit 1 ;;
#     -v|-verbose) verbose=1 && shift ;;
#       -d|-debug) debug=1 && shift ;;
#     -no-version-check) no_version_check=1 && shift ;;

docker run --rm --name $APP_NAME -p 8080:8080 \
   $MOUNT_OPTS \
   -e JAVA_OPTS=$OPT \
   -e DB_HOST=$DB_HOST \
   -e AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
   -e AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
   -e AWS_REGION=$AWS_REGION \
   -e S3_BUCKET=$S3_BUCKET \
   $DNS_OPT \
   $PRIVILEGED \
   $DOCKER_OPTS \
   $DOCKER $@
