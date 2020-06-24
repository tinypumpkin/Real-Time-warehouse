package com.atguigu.ods

import com.alibaba.fastjson.{JSON, JSONObject}
import com.atguigu.EsUtil.{MykafkaSink, MykafkaUtil, OffsetManager}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, OffsetRange}

object BaseDbMaxwell {

  def main(args: Array[String]): Unit = {

    val sparkConf: SparkConf = new SparkConf().setAppName("base_db_maxwell_app").setMaster("local[4]")
    val ssc = new StreamingContext(sparkConf,Seconds(5))
    val topic="GMALL0105_DB_M"
    val groupId="base_db_maxwel_group"
    val kafkaOffsetMap: Map[TopicPartition, Long] = OffsetManager.getoffset(topic,groupId)
    var recordInputStream: InputDStream[ConsumerRecord[String, String]]=null
    if(kafkaOffsetMap!=null&&kafkaOffsetMap.size>0){
      recordInputStream = MykafkaUtil.getKafkaStream(topic,ssc,kafkaOffsetMap,groupId)
    }else{
      recordInputStream = MykafkaUtil.getKafkaStream(topic,ssc)
    }

    //得到本批次的偏移量的结束位置，用于更新redis中的偏移量
    var  offsetRanges: Array[OffsetRange] = Array.empty[OffsetRange]
    val  inputGetOffsetDstream: DStream[ConsumerRecord[String, String]] = recordInputStream.transform { rdd =>
      offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges  //driver? executor?  //周期性的执行
      rdd
    }


    val jsonObjDstream: DStream[JSONObject] = inputGetOffsetDstream.map { record =>
      val jsonString: String = record.value()
      val jsonObj: JSONObject = JSON.parseObject(jsonString)
      jsonObj
    }




    jsonObjDstream.foreachRDD{rdd=>
      // 推回kafka
      rdd.foreach{jsonObj=>
        print(jsonObj.toString)
        if(jsonObj.getJSONObject("data")!=null && !jsonObj.getJSONObject("data").isEmpty
          &&("insert".equals(jsonObj.getString("type")) || "update".equals(jsonObj.getString("type")))
        ){
          val jsonString=jsonObj.getString("data")
          val tableName: String = jsonObj.getString("table")
          val topic="ODS_"+tableName.toUpperCase
          MykafkaSink.send(topic,jsonString)   //非幂等的操作 可能会导致数据重复
        }
      }

      OffsetManager.saveoffset(topic,groupId,offsetRanges)

    }

    ssc.start()
    ssc.awaitTermination()







  }



}
