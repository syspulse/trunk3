#!/bin/bash                                                                                                                                                                                            
CWD=`echo $(dirname $(readlink -f $0))`
cd $CWD
export APP_HOME=`pwd`

t=`pwd`;
APP=`basename "$t"`
CONF=`echo $APP | awk -F"-" '{print $2}'`

export SITE=${SITE:-$CONF}

MAIN=io.syspulse.haas.ingest.App

DOCKER_DEF=${DOCKER_DEF:-}
DOCKER_AWS=${DOCKER_AWS:-649502643044.dkr.ecr.eu-west-1.amazonaws.com}
DOCKER_VER=${DOCKER_VER:-latest}
S3_BUCKET=${S3_BUCKET:-haas-data-dev}

BUCKET_DIR=${BUCKET_DIR:-/data}

>&2 echo "app: $APP"
>&2 echo "site: $SITE"
>&2 echo "main: $MAIN"

if [ "$DOCKER" != "" ]; then
  >&2 echo "DOCKER: $DOCKER"
  case "$DOCKER" in
     "aws")
        docker run --rm -i \
            -v `pwd`/output:/output \
            -v $BUCKET_DIR:$BUCKET_DIR \
            -e AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY -e S3_BUCKET=$S3_BUCKET \
            --privileged \
            ${DOCKER_AWS}/syspulse/$APP:$DOCKER_VER \
            $@
        ;;
     "local"|"default")
        docker run --rm -i \
            -v `pwd`/output:/output \
            -v $BUCKET_DIR:$BUCKET_DIR \
            -e AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY -e S3_BUCKET=$S3_BUCKET \
            --privileged \
            ${DOCKER_DEF}syspulse/$APP:$DOCKER_VER \
            $@
        #docker run --rm -i --user 1000 -v `pwd`/output:/output ${DOCKER_AWS}/syspulse/$APP:$DOCKER_VER $@
        #docker run --rm -i -v `pwd`/output:/output ${DOCKER_AWS}/syspulse/$APP:$DOCKER_VER $@
        ;;
     *)
        docker run --rm -i \
            -v `pwd`/output:/output \
            -v $BUCKET_DIR:$BUCKET_DIR \
            -e AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY -e S3_BUCKET=$S3_BUCKET \
            --privileged \
            $DOCKER \
            $@
        ;;
  esac
   
else
   exec ../run-app.sh $APP $MAIN $@
fi

