package com.atguigu.EsUtil
import java.util

import com.atguigu.EsUtil.RedisUtil
import org.apache.kafka.common.TopicPartition
import org.apache.spark.streaming.kafka010.OffsetRange
import redis.clients.jedis.Jedis
object OffsetManager{
  //redis中读取偏移量
  def getoffset(topicName:String,groupId:String):Map[TopicPartition,Long]={
    //redis在偏移量保存格式
    val jedis:Jedis = RedisUtil.getJedisClient
    val offsetkey = "offset" + topicName + ":" + groupId
    val offsetMap:util.Map[String,String] = jedis.hgetAll(offsetkey)
    jedis.close()
    import scala.collection.JavaConversions._
    val kafkaOffsetMap:  Map[TopicPartition, Long] = offsetMap.map { case (patitionId, offset) =>
      println("加载分区偏移量："+patitionId +":"+offset  )
      (new TopicPartition(topicName, patitionId.toInt), offset.toLong)
    }.toMap
    kafkaOffsetMap
  }
  def saveoffset(topicName:String,groupId:String,offsetRange:Array[OffsetRange]): Unit ={
    val offsetkey = "offset:" + topicName + ":" + groupId
    val offsetMap:util.Map[String,String] = new util.HashMap()
    //转换结构 offsetRange-->offsetMap
    for(offset<-offsetRange){
      val partition:Int = offset.partition
      val untilOffset:Long = offset.untilOffset
      offsetMap.put(partition+"",untilOffset+"")
      println("写入分区:"+partition+"-偏移量起始"+offset.fromOffset+"-->偏移量结束:"+offset.untilOffset)
    }
    //写入redis--偏移量写入
    if (offsetMap!=null&&offsetMap.size()>0){
    val jedis:Jedis = RedisUtil.getJedisClient
    jedis.hmset(offsetkey,offsetMap)
    jedis.close()
    }
  }
}