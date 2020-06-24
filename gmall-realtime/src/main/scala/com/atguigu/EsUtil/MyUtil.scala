package com.atguigu.EsUtil
import java.util

import io.searchbox.client.config.HttpClientConfig
import io.searchbox.client.{JestClient, JestClientFactory}
import io.searchbox.core.{Bulk, BulkResult, Index, Search, SearchResult}
import org.elasticsearch.index.query.{BoolQueryBuilder, MatchQueryBuilder, TermQueryBuilder}
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.highlight.HighlightBuilder
import org.elasticsearch.search.sort.SortOrder

object MyUtil {
  var factory:JestClientFactory=null
  def getClient:JestClient ={
    if(factory==null)build();
    factory.getObject

  }

  def  build(): Unit ={
    factory=new JestClientFactory
    factory.setHttpClientConfig(new HttpClientConfig.Builder("http://hadoop100:9200" )
      //是否运行多线程
      .multiThreaded(true)
      //最大并发（取决于消费端cpu个数）
      .maxTotalConnection(20)
      //连接超时，读取超时
      .connTimeout(10000).readTimeout(10000).build())
  }

  //写入操作
  //插入单条数据
  def addDoc():Unit= {
    val jest = getClient
    //build设计模式
    //可转化为json对象    hashMap  或者样例类(此处通过样例类添加数据)
    val index = new Index.Builder(Movie("0104", "龙岭迷窟", "鬼吹灯")).index("movie0105").`type`("_doc").id("0104").build()
    val msg:String = jest.execute(index).getErrorMessage
    if(msg!=null)
      println(msg)
    jest.close()
  }
  def bulkDoc(sList:List[(String,Any)],indexName:String):Unit={
    if (sList!=null&&sList.size>0){
    val jest:JestClient = getClient
    val bulkbuilder = new Bulk.Builder
    for((id,source) <-sList) {
      val index = new Index.Builder(source).index(indexName).`type`("_doc").id(id)build()
      bulkbuilder.addAction(index)
    }
      val bulk:Bulk = bulkbuilder.build()
      val res:BulkResult = jest.execute(bulk)
      val items:util.List[BulkResult#BulkResultItem] = res.getItems
      println("保存到ES:"+items.size()+"条数")

      jest.close()
    }
  }
  //查询操作
  //查询片名中带red的演员是zhang han yu 的字段结果安douban排名降序排列
  def queryDoc(): Unit ={
    var query=""

    val jest:JestClient = getClient
    val ssb = new SearchSourceBuilder()
    val bqb = new BoolQueryBuilder
    bqb.must(new MatchQueryBuilder("name","red"))
    bqb.filter(new TermQueryBuilder("actorList.name.keyword","zhang han yu"))
    ssb.query(bqb)
    ssb.from(0).size(20)
    ssb.sort("doubanScore",SortOrder.DESC)
    ssb.highlight(new HighlightBuilder().field("name"))
    query=ssb.toString
    println(query)


    val search = new Search.Builder(query).addIndex("movie_index").addType("movie")build()
    val res = jest.execute(search)
    val rsList: util.List[SearchResult#Hit[util.Map[String,Any],Void]] = res.getHits(classOf[util.Map[String, Any]])


    import scala.collection.JavaConversions._
    for (rl <- rsList ) {
      println(rl.source.mkString(","))
    }
    jest.close()
  }

  def main(args: Array[String]): Unit = {
    //    addDoc()
    queryDoc()
  }

case class Movie(id:String,movie_name:String,name:String)
}
