package com.atguigu.publisher.servier.impl;

import com.atguigu.publisher.servier.EsServer;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EsServiceIpm implements EsServer {
    @Autowired
    JestClient jestClient;

    @Override
    public Long getDau(String data) {
    String indexname="gmall0105_dau_info_"+data+"-query";
        SearchSourceBuilder sbuild = new SearchSourceBuilder();
        sbuild.query(new MatchAllQueryBuilder());
        Search search = new Search.Builder(sbuild.toString()).addIndex(indexname).addType("_doc").build();
        try{
            SearchResult searRes = jestClient.execute(search);
            return searRes.getTotal();
        }
        catch (IOException e){
            e.printStackTrace();
            throw new RuntimeException("es查询异常");
        }
    }

    @Override
    public Map getDauHour(String data) {
        String indexName = "gmall0105_dau_info_"+data+"-query";
        //构造查询语句
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        TermsBuilder aggBuilder = AggregationBuilders.terms("groupby_hr").field("hr").size(24);
        searchSourceBuilder.aggregation(aggBuilder);

        Search search = new Search.Builder(searchSourceBuilder.toString()).addIndex(indexName).addType("_doc").build();
        try {
            SearchResult searchResult = jestClient.execute(search);
            //封装返回结果
            Map<String,Long> aggMap=new HashMap<>();
            if(searchResult.getAggregations().getTermsAggregation("groupby_hr")!=null){
                List<TermsAggregation.Entry> buckets = searchResult.getAggregations().getTermsAggregation("groupby_hr").getBuckets();
                for (TermsAggregation.Entry bucket : buckets) {
                    aggMap.put( bucket.getKey(),bucket.getCount());
                }
            }
            return aggMap;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("es 查询异常");
        }
    }
}
