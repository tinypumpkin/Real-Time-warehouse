package com.atguigu.App

import java.lang
import java.text.SimpleDateFormat
import java.util.Date

import com.atguigu.EsUtil.{MyUtil, MykafkaUtil, OffsetManager, RedisUtil}
import com.alibaba.fastjson.{JSON, JSONObject}
import com.atguigu.EsUtil
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, OffsetRange}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import redis.clients.jedis.Jedis

import scala.collection.mutable.ListBuffer

object DauApp {
  def main(args: Array[String]): Unit = {
    val sparkConf = new SparkConf().setAppName("dau_app").setMaster("local[4]")
    val scc = new StreamingContext(sparkConf, Seconds(5))
    val topic = "GMALL_STARTUP_0105"
    val groupId = "dau_group"
    val kafkaoffsetMap:Map[TopicPartition,Long] = OffsetManager.getoffset(topic, groupId)
    var recordInputStream:InputDStream[ConsumerRecord[String,String]] =null
    if(kafkaoffsetMap!=null&&kafkaoffsetMap.size>0){
      //偏移量大于0，有值加载，手动维护偏移量
      recordInputStream=MykafkaUtil.getKafkaStream(topic,scc,kafkaoffsetMap,groupId)
    }else{
      //没加载出数据从头加载
      recordInputStream=MykafkaUtil.getKafkaStream(topic,scc)
    }
//    recordInputStream.map(_.value()).print()

    //得到本批次的偏移量结束位置,用于更新redis的偏移量
    var offsetRange:Array[OffsetRange] = Array.empty[OffsetRange]
    val startupInputgetoffsetDS:DStream[ConsumerRecord[String,String]] = recordInputStream.transform { rdd =>
      offsetRange = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
      rdd
    }

    val jsonDstream:DStream[JSONObject] = startupInputgetoffsetDS.map {
      record =>
        val jsonstr: String = record.value()
        val jsonobj = JSON.parseObject(jsonstr)
        val ts: lang.Long = jsonobj.getLong("ts")
        val dtString: String = new SimpleDateFormat("yyyy-MM-dd HH").format(new Date(ts))
        val dtHour: Array[String] = dtString.split(" ")

        jsonobj.put("dt", dtHour(0))
        jsonobj.put("hr", dtHour(1))
        jsonobj
    }
    jsonDstream

    //去重利用redis保存今天访问过系统的用户清单
    val filDs:DStream[JSONObject] = jsonDstream.mapPartitions {
      jsonObjItr =>
        val jedis:Jedis = RedisUtil.getJedisClient //一个分区只申请一次连接
        val fiList = new ListBuffer[JSONObject]()
        val jsList:List[JSONObject] = jsonObjItr.toList
        println("过滤前"+jsList.size)
        for (jsonObj <- jsList) {
          val dt: String = jsonObj.getString("dt")
          val mid:String = jsonObj.getJSONObject("common").getString("mid")
          val dauKey="dau"+dt
          val isnew:lang.Long = jedis.sadd(dauKey, mid)//不存在返回1已存在返回0
        if (isnew==1L){
          fiList+=jsonObj}
        }
        jedis.close()
        println("过滤后"+fiList.size)
        fiList.toIterator
    }
    filDs.foreachRDD { rdd =>
      rdd.foreachPartition { itor =>
        val list: List[JSONObject] = itor.toList
        //把源数据 转换成为要保存的数据格式
        val dList: List[(String,DauInfo)] = list.map { jsobj =>
          val commonJSONObj: JSONObject = jsobj.getJSONObject("common")
          val dauInfo = DauInfo(commonJSONObj.getString("mid"),
            commonJSONObj.getString("uid"),
            commonJSONObj.getString("ar"),
            commonJSONObj.getString("ch"),
            commonJSONObj.getString("vc"),
            jsobj.getString("dt"),
            jsobj.getString("hr"),
            "00",
            jsobj.getLong("ts")
          )
          //dauInfo
          (dauInfo.mid,dauInfo)
        }

        val dt: String = new SimpleDateFormat("yyyy-MM-dd").format(new Date())
        MyUtil.bulkDoc(dList, "gmall0105_dau_info_" + dt)
      }
      //偏移量提交区
      OffsetManager.saveoffset(topic, groupId, offsetRange)
    }

    scc.start()
    scc.awaitTermination()
  }
}

case class DauInfo(
                    mid:String,
                    uid:String,
                    ar:String,
                    ch:String,
                    vc:String,
                    var dt:String,
                    var hr:String,
                    var mi:String,
                    ts:Long) {

}
