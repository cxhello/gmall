package com.cxhello.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.cxhello.gmall.bean.SkuInfo;
import com.cxhello.gmall.bean.SkuLsInfo;
import com.cxhello.gmall.bean.SkuLsParams;
import com.cxhello.gmall.bean.SkuLsResult;
import com.cxhello.gmall.config.RedisUtil;
import com.cxhello.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author CaiXiaoHui
 * @create 2019-07-10 0:16
 */
@Service
public class ListServiceImpl implements ListService {

    @Autowired
    private JestClient jestClient;

    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo) {

        //保存
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();

        //执行动作
        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {

        String query = makeQueryStringForSearch(skuLsParams);

        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult = null;
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }

        SkuLsResult skuLsResult = makeResultForSearch(skuLsParams,searchResult);

        return skuLsResult;
    }


    public String makeQueryStringForSearch(SkuLsParams skuLsParams){
        //1.创建查询build
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        
        if(skuLsParams.getKeyword()!=null){
            MatchQueryBuilder ma = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            boolQueryBuilder.must(ma);
            //2.设置高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            //设置高亮字段
            highlightBuilder.field("skuName");
            highlightBuilder.preTags("<span style='color:red'>");
            highlightBuilder.postTags("</span>");
            //将高亮结果放入查询器中
            searchSourceBuilder.highlight(highlightBuilder);
        }
        //3.设置三级分类ID查询
        if(skuLsParams.getCatalog3Id()!=null){
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            boolQueryBuilder.filter(termQueryBuilder);
        }
        //4.设置属性值
        if(skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                String valueId = skuLsParams.getValueId()[i];
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        searchSourceBuilder.query(boolQueryBuilder);
        //5.设置分页
        int from = (skuLsParams.getPageNo()-1)*skuLsParams.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(skuLsParams.getPageSize());
        //6.设置按照热度
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        //7.设置聚合
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");

        searchSourceBuilder.aggregation(groupby_attr);

        String query = searchSourceBuilder.toString();

        System.out.println(query);

        return query;
    }

    /**
     * 制作返回结果集
     * @param skuLsParams
     * @param searchResult
     * @return
     */
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        SkuLsResult skuLsResult = new SkuLsResult();

        //1.List<SkuLsInfo> skuLsInfoList;
        ArrayList<SkuLsInfo> skuLsInfoArrayList = new ArrayList<>();

        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        if(hits!=null && hits.size()>0){
            //循环遍历取出skuLsInfo
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                SkuLsInfo source = hit.source;
                //获取高亮的skuName
                if(hit.highlight!=null && hit.highlight.size()>0){
                    List<String> skuNameList = hit.highlight.get("skuName");

                    String skuNameHI = skuNameList.get(0);

                    source.setSkuName(skuNameHI);
                }
                skuLsInfoArrayList.add(source);
            }
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoArrayList);

        //2.long total;
        skuLsResult.setTotal(searchResult.getTotal());

        //long totalPages;
        //long totalPages = skuLsResult.setTotalPages(searchResult.getTotal()%skuLsParams.getPageSize()>0?searchResult.getTotal()/skuLsParams.getPageSize()+1:searchResult.getTotal()/skuLsParams.getPageSize());
        long totalPages = (searchResult.getTotal()+skuLsParams.getPageSize()-1)/skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPages);

        //3.List<String> attrValueIdList;
        ArrayList<String> arrayList = new ArrayList<>();

        //给arrayList赋值
        //获取平台属性值ID
        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");

        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        if(buckets!=null && buckets.size()>0){
            for (TermsAggregation.Entry bucket : buckets) {
                String valueId = bucket.getKey();
                arrayList.add(valueId);
            }
        }
        skuLsResult.setAttrValueIdList(arrayList);

        return skuLsResult;
    }

    @Override
    public void incrHotScore(String skuId) {
        //1.获取jedis
        Jedis jedis = redisUtil.getJedis();

        String hotKey = "hotScore";

        Double result = jedis.zincrby(hotKey, 1, "skuId:" + skuId);

        //每10次更新1次ES
        if(result%10==0){
            //调用更新ES的方法
            updateHotScore(skuId,  Math.round(result));
        }

        jedis.close();

    }

    /**
     * 更新ES
     * @param skuId
     * @param hotScore
     */
    private void updateHotScore(String skuId, long hotScore) {

        String updateDsl = "{\n" +
                "  \"doc\": {\n" +
                "    \"hotScore\":"+hotScore+"\n" +
                "  }\n" +
                "}";

        Update update = new Update.Builder(updateDsl).index(ES_INDEX).type(ES_TYPE).id(skuId).build();

        //System.out.println(update.toString());

        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
