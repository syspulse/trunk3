#!/bin/bash                                                                                                                                                                                            
ROOT=`echo $(dirname $(readlink -f $0))`

NAME=`basename $PWD | sed 's/\-/_/g'`
DIR=$PWD

pushd $ROOT
#sbt -error ";project $NAME; export dependencyClasspath" 2>/dev/null >$DIR/CLASSPATH
sbt  -error ";project $NAME; export dependencyClasspath" >$DIR/CLASSPATH
popd
