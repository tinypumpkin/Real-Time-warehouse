# canal安装与配置
### 配置Mysql
+ 修改配置文件(my.cnf)
```bash
vim /etc/my.cnf
```
```cnf
server-id= 1
log-bin=mysql-bin
binlog_format=row
#自定义的topic
binlog-do-db=GMALL0105_DB_C
```
+ 重启MySQL
```bash
systemctl restart mysqld
```
+ 建库执行sql脚本倒数据
```sql
create databases gmall0105;
```
```bash
#appdb下创建执行jar包导入数据
java -jar gmall2020-mock-db-2020-05-18.jar
```
+ 给canal单独创建一个用户
```sql
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%' IDENTIFIED BY 'canal' ;
```
### Canal环境的安装配置
+ 下载解压
```bash
mkdir canal
cd canal
tar -zxvf canal.xxx.tar.gz  
```
>修改canal的配置文件
```bash
cd /opt/module/canal/conf
```
+ canal.properties
```bash
vim canal.properties
```
```properties
#canal高可用配置zookeeper
canal.zkServer=hadoop100:2181,hadoop101:2181,hadoop102:2181
# 服务器模式kafka
canal.serverMode = kafka
#########  MQ  #############
canal.mq.servers = hadoop100:9092,hadoop101:9092,hadoop102:9092
```
+ example文件下
```bash
vim example/instance.properties
```
```properties
canal.instance.master.address=hadoop100:3306
# mq config
canal.mq.topic=kafka会话
canal.mq.partitionsNum=4
```
>客户端测试
```bash
kafka-console-consumer.sh --bootstrap-server hadoop100:9092 --topic GMALL0105_DB_C
```