# MaxWell
开源，用java编写的Mysql实时抓取软件。 其抓取的原理也是基于binlog。
### 工具对比(与canal)
1. Maxwell 没有 Canal那种server+client模式，只有一个server把数据发送到消息队列或redis。


2. Maxwell 有一个亮点功能，就是Canal只能抓取最新数据，对已存在的历史数据没有办法处理。而Maxwell有一个bootstrap功能，可以直接引导出完整的历史数据用于初始化，非常好用。


3. Maxwell不能直接支持HA（高可用），但是它支持断点还原，即错误解决后重启继续上次点儿读取数据。 


4. Maxwell只支持json格式，而Canal如果用Server+client模式的话，可以自定义格式。


5. Maxwell比Canal更加轻量级。

## Maxwell的安装
>使用前准备
+ 在数据库(MySQL)中建立一个maxwell库用于存储Maxwell的元数据。
```sql
CREATE DATABASE maxwell ;
```
+ 分配账号可以操作该数据库
```sql
--给maxwell库分配全部权限
GRANT ALL  ON maxwell.* TO 'maxwell'@'%' IDENTIFIED BY 'maxwell';
```
+ 分配这个账号可以监控其他数据库的权限
```sql
GRANT  SELECT ,REPLICATION SLAVE , REPLICATION CLIENT  ON *.* TO maxwell@'%'
```
>安装
+ 下载解压
```bash
tar -zxvf maxwell-xxx.tar.gz
mv maxwell-xxx maxwell
cd maxwell
```
+ 配置文件config.properties 
```bash
cp config.properties.example config.properties
vim config.properties
```
```properties
producer=kafka
kafka.bootstrap.servers=hadoop100:9092,hadoop101:9092,hadoop102:9092
kafka_topic=tp1

host=hadoop100
user=maxwell
password=maxwell
<!-- 客户端唯一标识 -->
client_id=maxwell_1
```
+ 启动程序-启动时指定maxwell.properties文件
```bash
/opt/module/maxwell-xxx/bin/maxwell --config  /xxx/maxwell.properties >/dev/null 2>&1 &
```

+ 使用Maxwell监控抓取MySql数据
```bash
vim /opt/module/maxwell/maxwell.properties 
```
```properties
producer=kafka
kafka.bootstrap.servers=hadoop100:9092,hadoop101:9092,hadoop102:9092
kafka_topic=tp1

host=hadoop100
user=maxwell
password=maxwell
<!-- 客户端唯一标识 -->
client_id=maxwell_1
```
+ 修改或插入mysql数据，并消费kafka进行观察
```bash
bin/kafka-topics.sh --create --topic tp1 --zookeeper hadoop100:2181,hadoop101:2181,hadoop102:2181 --partitions 12 --replication-factor 1
```
+ 执行测试语句
```sql
INSERT INTO user_info VALUES(30,'zhang3','13810001010'),(31,'li4','1389999999');
```
### 数据特点对比：
#### 日志结构
+ canal 

每一条SQL会产生一条日志，如果该条Sql影响了多行数据，则已经会通过集合的方式归集在这条日志中。（即使是一条数据也会是数组结构）

+ maxwell

以影响的数据为单位产生日志，即每影响一条数据就会产生一条日志。如果想知道这些日志是否是通过某一条sql产生的可以通过xid进行判断，相同的xid的日志来自同一sql。

 #### 数字类型
原始数据是数字类型时-->maxwell保持原类型，canal一律转换为字符串。

#### 带原始数据字段定义
canal数据中会带入原始数据表结构, maxwell没有表结构，更简洁。

