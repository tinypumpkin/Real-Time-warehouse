#!/bin/bash
JAVA_BIN=/opt/module/jdk1.8.0_212/bin/java
APPNAME=gmall2020-mock-log-2020-05-10.jar
 
case $1 in
 "start")
   {
 
    for i in hadoop100 
    do
     echo "======== $i 开启数据生成==============="
    ssh $i  "$JAVA_BIN -Xms32m -Xmx64m  -jar /opt/module/applog/$APPNAME >/dev/null 2>&1  &"
    done
  };;
  "stop")
  { 
    for i in  hadoop100 
    do
     echo "======== $i 关闭数据生成==============="
     ssh $i "ps -ef|grep $APPNAME |grep -v grep|awk '{print \$2}'|xargs kill" >/dev/null 2>&1
    done
  };;
esac
 
