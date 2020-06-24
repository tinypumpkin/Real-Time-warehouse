# canal
canal是用java开发的基于数据库增量日志解析，提供增量数据订阅&消费的中间件。目前，canal主要支持了MySQL的binlog解析，解析完成后才利用canal client 用来处理获得的相关数据。
### 使用场景
+ 原始场景:阿里otter中间件的一部分(otter是异地数据库之间的同步框架)
+ 常用场景: 
1. 更新缓存
2. 抓取业务数据新增变化表，用于制作拉链表
3. 抓取业务表的新增变化数据，用于制作实时统计
### 工作原理（主从复制--模拟从机）
+ Master主库将改变记录，写到二进制日志(binary log)中

+ Slave从库向mysql master发送dump协议，将master主库的binary log events拷贝到它的中继日志(relay log)

+ Slave从库读取并重做中继日志中的事件，将改变的数据同步到自己的数据库。
### binlog
MySQL的二进制日志可以说是MySQL最重要的日志了，它记录了所有的DDL和DML(除了数据查询语句)语句，以事件形式记录，还包含语句所执行的消耗的时间，MySQL的二进制日志是事务安全型的。

>binlog的开启

binlog日志的前缀是mysql-bin  ，以后生成的日志文件就是 mysql-bin.123456 的文件后面的数字按顺序生成。 每次mysql重启或者到达单个文件大小的阈值时，新生一个文件，按顺序编号。

>binlog的分类设置
+ statement 

语句级 binlog会记录每次一执行写操作的语句,相对row模式节省空间，但是可能产生不一致性，比如update  tt set create_date=now() 如果用binlog日志进行恢复，由于执行时间不同可能产生的数据就不同。

优点： 节省空间

缺点： 有可能造成数据不一致。
+ row 
行级 binlog会记录每次操作后每行记录的变化。

优点：保持数据的绝对一致性(遇到批量执行sql会出现大量数据冗余)

缺点：占用较大空间。

+ mixed
statement的升级版，一定程度上解决了，因为一些情况而造成的statement模式不一致问题

在某些情况下譬如：
1. 当函数中包含 UUID() 时；
2. 包含 AUTO_INCREMENT 字段的表被更新时；
3. 执行 INSERT DELAYED 语句时；
4. 用 UDF 时；

会按照 ROW的方式进行处理

优点：节省空间，同时兼顾了一定的一致性。

缺点：还有些极个别情况依旧会造成不一致，另外statement和mixed对于需要对binlog的监控的情况都不方便。

