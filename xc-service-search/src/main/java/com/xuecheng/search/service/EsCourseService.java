package com.xuecheng.search.service;

import com.xuecheng.framework.domain.course.CoursePub;
import com.xuecheng.framework.domain.course.TeachplanMediaPub;
import com.xuecheng.framework.domain.search.CourseSearchParam;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.TermQuery;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 * @version 1.0
 **/
@Service
public class EsCourseService {

    @Value("${xuecheng.course.index}")
    private String courseIndex;
    @Value("${xuecheng.course.type}")
    private String courseType;
    @Value("${xuecheng.course.source_field}")
    private String course_source_field;
    @Value("${xuecheng.media.index}")
    private String mediaIndex;
    @Value("${xuecheng.media.type}")
    private String mediaType;
    @Value("${xuecheng.media.source_field}")
    private String media_source_field;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 课程综合搜索
     * @author : yechaoze
     * @date : 2019/7/10 13:13
     * @param page :
     * @param size :
     * @param courseSearchParam :
     * @return : com.xuecheng.framework.model.response.QueryResponseResult<com.xuecheng.framework.domain.course.CoursePub>
     */
    public QueryResponseResult<CoursePub> list(int page, int size, CourseSearchParam courseSearchParam){
        if (courseSearchParam==null){
            courseSearchParam=new CourseSearchParam();
        }
        //创建搜索请求对象
        SearchRequest searchRequest=new SearchRequest(courseIndex);
        //设置类型
        searchRequest.types(courseType);

        SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder();
        String[] split = course_source_field.split(",");
        //搜索源字段过滤
        searchSourceBuilder.fetchSource(split,new String[]{});
        //创建布尔搜索
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //按关键字查询
        if (StringUtils.isNotEmpty(courseSearchParam.getKeyword())){
            MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery(courseSearchParam.getKeyword(), "name", "description", "teachplan")
                    //设置匹配占比
                    .minimumShouldMatch("70%")
                    //提升boost
                    .field("name", 10);
            boolQueryBuilder.must(multiMatchQueryBuilder);
        }
        //根据一级分类查询
        if(StringUtils.isNotEmpty(courseSearchParam.getMt())){
            boolQueryBuilder.filter(QueryBuilders.termQuery("mt",courseSearchParam.getMt()));
        }
        //根据二级分类查询
        if(StringUtils.isNotEmpty(courseSearchParam.getSt())){
            boolQueryBuilder.filter(QueryBuilders.termQuery("st",courseSearchParam.getSt()));
        }
        //根据难度等级查询
        if(StringUtils.isNotEmpty(courseSearchParam.getGrade())){
            boolQueryBuilder.filter(QueryBuilders.termQuery("grade",courseSearchParam.getGrade()));
        }
        searchSourceBuilder.query(boolQueryBuilder);

        //设置分页参数
        if (page<=0){
            page=1;
        }
        if(size<=0){
            size=12;
        }
        int from=(page-1)*size;
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(size);

        //设置高亮
        HighlightBuilder highlightBuilder=new HighlightBuilder();
        highlightBuilder.preTags("<font class='eslight'>");
        highlightBuilder.postTags("</font>");
        highlightBuilder.field("name");
        searchSourceBuilder.highlighter(highlightBuilder);

        searchRequest.source(searchSourceBuilder);

        QueryResult<CoursePub> queryResult=new QueryResult<>();
        List<CoursePub> list=new ArrayList<>();
        try {
            //执行搜索
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest);
            SearchHits searchHits = searchResponse.getHits();
            //取总记录数
            long totalHits = searchHits.totalHits;
            SearchHit[] searchHitsHits = searchHits.getHits();
            queryResult=new QueryResult<>();
            queryResult.setTotal(totalHits);
            for (SearchHit hit:searchHitsHits){
                CoursePub coursePub=new CoursePub();
                //获取源文档
                Map<String, Object> source = hit.getSourceAsMap();
                //取出id
                String id= (String) source.get("id");
                coursePub.setId(id);
                //取name
                String name = (String) source.get("name");
                //取高亮的字段
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                if (highlightFields!=null){
                    HighlightField highlightField = highlightFields.get("name");
                    if (highlightField!=null){
                        Text[] texts = highlightField.getFragments();
                        StringBuffer stringBuffer=new StringBuffer();
                        for (Text text:texts){
                            stringBuffer.append(text);
                        }
                        name=stringBuffer.toString();
                    }
                }
                coursePub.setName(name);
                //取图片
                String pic = (String) source.get("pic");
                coursePub.setPic(pic);
                //取新价格
                Double price = null;
                try {
                    if(source.get("price")!=null ){
                        price = (Double) source.get("price");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                coursePub.setPrice(price);
                //取旧价格
                Double price_old = null;
                try {
                    if(source.get("price_old")!=null ){
                        price_old = (Double) source.get("price_old");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                coursePub.setPrice_old(price_old);
                //将coursePub放入list
                list.add(coursePub);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        queryResult.setList(list);
        QueryResponseResult<CoursePub> coursePubQueryResponseResult = new QueryResponseResult<>(CommonCode.SUCCESS, queryResult);
        return coursePubQueryResponseResult;
    }

    /**
     * 使用ES的客户端向ES请求查询索引信息
     * @author : yechaoze
     * @date : 2019/7/22 12:53
     * @param id : 
     * @return : java.util.Map<java.lang.String,com.xuecheng.framework.domain.course.CoursePub>
     */
    public Map<String,CoursePub> getAll(String id) {
        //定义搜索请求对象
        SearchRequest searchRequest=new SearchRequest(courseIndex);
        //指定类型
        searchRequest.types(courseType);

        //定义sourceBuilder
        SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder();
        //查询条件，根据课程id查询
        searchSourceBuilder.query(QueryBuilders.termQuery("id",id));
        searchRequest.source(searchSourceBuilder);

        //定义返回值对象
        CoursePub coursePub=new CoursePub();
        Map<String,CoursePub> map=new HashMap<>();
        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest);
            SearchHits responseHits = searchResponse.getHits();
            SearchHit[] searchHits = responseHits.getHits();
            for (SearchHit hits:responseHits){
                Map<String, Object> sourceAsMap = hits.getSourceAsMap();
                String courseId = (String) sourceAsMap.get("id");
                String name = (String) sourceAsMap.get("name");
                String grade = (String) sourceAsMap.get("grade");
                String charge = (String) sourceAsMap.get("charge");
                String pic = (String) sourceAsMap.get("pic");
                String description = (String) sourceAsMap.get("description");
                String teachplan = (String) sourceAsMap.get("teachplan");
                coursePub.setId(courseId);
                coursePub.setName(name);
                coursePub.setPic(pic);
                coursePub.setGrade(grade);
                coursePub.setTeachplan(teachplan);
                coursePub.setDescription(description);
                map.put(courseId,coursePub);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }


    /**
     * 根据多个课程计划id查询媒资信息
     * @author : yechaoze
     * @date : 2019/7/23 22:32
     * @param teachplanIds :
     * @return : com.xuecheng.framework.domain.course.TeachplanMediaPub
     */
    public QueryResponseResult<TeachplanMediaPub> getMedia(String[] teachplanIds) {
        //创建搜索请求对象
        SearchRequest searchRequest=new SearchRequest(mediaIndex);
        //设置类型
        searchRequest.types(mediaType);

        //创建sourceBulider
        SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder();
        //设置使用termsQuery 多个课程计划id查询
        searchSourceBuilder.query(QueryBuilders.termsQuery("teachplan_id",teachplanIds));
        //过滤源字段
        String[] includes = media_source_field.split(",");
        searchSourceBuilder.fetchSource(includes,new String[]{});
        searchRequest.source(searchSourceBuilder);
        //使用客户端进行搜索请求
        List<TeachplanMediaPub> teachplanMediaPubList=new ArrayList<>();
        long total=0;
        try {
            //执行搜索
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest);
            SearchHits hits = searchResponse.getHits();
            SearchHit[] searchHits = hits.getHits();
            total = hits.getTotalHits();
            for (SearchHit list:searchHits){
                TeachplanMediaPub teachplanMediaPub=new TeachplanMediaPub();
                Map<String, Object> sourceAsMap = list.getSourceAsMap();
                //取出课程计划媒资信息
                String courseId = (String) sourceAsMap.get("courseid");
                String media_id = (String) sourceAsMap.get("media_id");
                String media_url = (String) sourceAsMap.get("media_url");
                String teachplan_id = (String) sourceAsMap.get("teachplan_id");
                String media_fileoriginalname = (String) sourceAsMap.get("media_fileoriginalname");

                teachplanMediaPub.setCourseId(courseId);
                teachplanMediaPub.setMediaUrl(media_url);
                teachplanMediaPub.setMediaFileOriginalName(media_fileoriginalname);
                teachplanMediaPub.setMediaId(media_id);
                teachplanMediaPub.setTeachplanId(teachplan_id);
                teachplanMediaPubList.add(teachplanMediaPub);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        QueryResult<TeachplanMediaPub> queryResult=new QueryResult<>();
        queryResult.setList(teachplanMediaPubList);
        queryResult.setTotal(total);
        QueryResponseResult<TeachplanMediaPub> queryResponseResult=new QueryResponseResult(CommonCode.SUCCESS,queryResult);
        return queryResponseResult;
    }
}
