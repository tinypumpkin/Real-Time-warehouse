#! /bin/bash

PROJECT=opt/module/realtime-warehouse
APPNAME=gmall0105-logger-0.0.1-SNAPSHOT.jar
SERVER_PORT=8081
case $1 in
"start"){
  echo "========NGINX==============="
  /opt/module/nginx/sbin/nginx
	for i in hadoop100 hadoop101 hadoop102 
	do
  echo "========$i 开启收集队列==============="
  ssh $i "java -Xms32m -Xmx64m  -jar /$PROJECT/$APPNAME --server.port=$SERVER_PORT"
	done
};;
"stop"){
	for i in hadoop100 hadoop101 hadoop102
	do
     echo "========$i 关闭收集队列==============="
     ssh $i "ps -ef|grep $APPNAME |grep -v grep|awk '{print \$2}'|xargs kill" >/dev/null 2>&1
	done
  echo "======== NGINX==============="
  /opt/module/nginx/sbin/nginx  -s stop
};;
esac
