#!/bin/bash
JAVA_BIN=/opt/module/jdk1.8.0_212/bin/java
PROJECT=realtime-warehouse
APPNAME=gmall0105-logger-0.0.1-SNAPSHOT.jar
SERVER_PORT=8081
 
case $1 in
 "start")
   {
     echo "========NGINX==============="
    /opt/module/nginx/sbin/nginx
 
    for i in hadoop100 hadoop101 hadoop102
    do
     echo "========: $i==============="
    ssh $i  "$JAVA_BIN -Xms32m -Xmx64m  -jar /$PROJECT/$APPNAME --server.port=$SERVER_PORT >/dev/null 2>&1  &"
    done
  };;
  "stop")
  { 
    for i in  hadoop102 hadoop101 hadoop100
    do
     echo "========: $i==============="
     ssh $i "ps -ef|grep $APPNAME |grep -v grep|awk '{print \$2}'|xargs kill" >/dev/null 2>&1
    done
     echo "======== NGINX==============="
    /opt/module/nginx/sbin/nginx  -s stop
 
  };;
   esac
