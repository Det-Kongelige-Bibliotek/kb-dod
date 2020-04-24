#!/usr/bin/env bash

SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))

set -e

pushd $SCRIPT_DIR > /dev/null
PROJECTNAME=$(basename $SCRIPT_DIR)

(mvn clean package -Psbprojects-nexus -DskipTests=true) || exit 1

TOMCAT_VERSION=9.0.33
#TOMCAT_VERSION=8.5.51
wget -N "http://mirrors.dotsrc.org/apache/tomcat/tomcat-$(echo $TOMCAT_VERSION | cut -d'.' -f1)/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz"
[ -d $SCRIPT_DIR/apache-tomcat-$TOMCAT_VERSION ] || tar -xvzf apache-tomcat-$TOMCAT_VERSION.tar.gz

TOMCAT_HOME=$SCRIPT_DIR/apache-tomcat-$TOMCAT_VERSION
$TOMCAT_HOME/bin/shutdown.sh

rm -rf $TOMCAT_HOME/webapps/*
rm -rf $TOMCAT_HOME/logs/*

mkdir -p "$TOMCAT_HOME/conf/Catalina/localhost/"
cp $SCRIPT_DIR/conf/context.xml $TOMCAT_HOME/conf/Catalina/localhost/$PROJECTNAME.xml

#TODO make this automatic
#ssh -L 1025:smtp.statsbiblioteket.dk:25 abr@wonky

sleep 5

warfile=$(ls -1 target/*.war | head -n 1 | xargs -r -i basename {})

export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-11/}
export JAVA_OPTS="$JAVA_OPTS -Dproject.home=$SCRIPT_DIR -Dproject.build.finalName=$warfile"
export JPDA_ADDRESS="localhost:7500"

exec $TOMCAT_HOME/bin/catalina.sh jpda run
