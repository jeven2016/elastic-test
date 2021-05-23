package wzjtech.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.ParsedAvg;
import org.elasticsearch.search.aggregations.metrics.ParsedSingleValueNumericMetricsAggregation;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import reactor.core.publisher.Mono;
import wzjtech.test.spring.entity.Book;
import wzjtech.test.spring.repo.BookRep;

@SpringBootTest
public class BookTest {

  @Autowired
  BookRep bookRep;

  @Autowired
  ReactiveElasticsearchTemplate template;

  @Autowired
  RestHighLevelClient client;

  @Autowired
  ObjectMapper objectMapper;

  private Set<Book> getBooks() throws IOException {
    String path = "books.xml";
    boolean isFile = Paths.get(path).toFile().isFile();
    if (!isFile) {
      throw new IllegalArgumentException("File doesn't exist");
    }
    XmlMapper xmlMapper = new XmlMapper();
    File outputFile = new File(path);

    Set<Book> set = xmlMapper.readValue(outputFile, new TypeReference<Set<Book>>() {
    });
    return set;
  }

  /**
   * Import some data
   *
   * @throws IOException
   */
  @Test
  public void testImport() throws IOException {
    var books = getBooks();
    template.saveAll(books, IndexCoordinates.of("books")).blockLast();
  }

  // with CriteriaQuery
  @Test
  public void searchByName() {
    var criteria = new Criteria("name").is("狂热").or("name").is("狂热2");
    var query = new CriteriaQuery(criteria);
    template
        .search(query, Book.class)
        .doOnNext(
            searchHit -> {
              var book = searchHit.getContent();
              System.out.println("name=" + book.getName());
            })
        .blockLast();
  }

  @Test
  public void searchByName_nativeQuery() {
    /*
      {
       "from":0,
       "size":10000,
       "query":{
          "match":{
             "name":{
                "query":"狂热",
                "operator":"OR",
                "prefix_length":0,
                "max_expansions":50,
                "fuzzy_transpositions":true,
                "lenient":false,
                "zero_terms_query":"NONE",
                "auto_generate_synonyms_phrase_query":true,
                "boost":1.0
             }
          }
       },
       "version":true
    }
         */
    var queryBuilder = new NativeSearchQueryBuilder();

    // 会进行分词查找，比如“女业务”会匹配"女"和”业务“记录
    Query query = queryBuilder.withQuery(QueryBuilders.matchQuery("name", "女业务员")).build();

    search(query);
  }

  /*
  比如"我的宝马多少马力"，一个文档"我的保时捷马力不错"也会被搜索出来，那么想要精确匹配所有同时包含"宝马 多少 马力"的文档怎么做？就要使用 match_phrase 了
    */
  @Test
  public void testPhraseQuery() {
    var query =
        new NativeSearchQueryBuilder()
            // 完全匹配可能比较严，我们会希望有个可调节因子，少匹配一个也满足，那就需要使用到slop
            .withQuery(QueryBuilders.matchPhraseQuery("name", "女业务").slop(1))
            .build();

    search(query);
  }

  /*
    GET /books/_search
  {
    "query": {
      "bool": {
        "must": [
          { "match": { "name":   "女业务"        }}
        ],
        "filter": [
          { "range":  { "picCount": {"gte": 1000} }}
        ]
      }
    }
  }
     */
  @Test
  public void testMatchWithFilter() {
    var query =
        new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchQuery("name", "女业务"))
            // do further query based on previous result
            .withFilter(QueryBuilders.rangeQuery("picCount").gte(1000))
            .build();
    query.setMaxResults(10);
    search(query);

    // or with bool query
    query =
        new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("name", "女业务")))
            .withFilter(QueryBuilders.rangeQuery("picCount").gte(1000))
            .build();
    query.setMaxResults(10);
    search(query);
  }

  /*
  多个字段匹配一个值
   */
  @Test
  public void testMultiMatch() {
    var query =
        new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.multiMatchQuery("女业务", "name", "url"))
            .build();
    search(query);
  }

  /*
   it works for keyword but seems not working for analyzed text
  */
  @Test
  public void testWildcardQuery() {
    var query =
        new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.wildcardQuery("url", "*OAjn?L*")) // ? means only one char
            .build();

    search(query);
  }

  /*
  Pageable: search_after, the sort is required, cannot got to a specific page.
  */
  @Test
  public void testPageable() throws IOException {
    var pageLimit = 5;
    var currentPage = 0;

    // page: 5 items/page
    // firstly, query the first page that no search_after set
    var sb =
        new SearchSourceBuilder()
            .query(QueryBuilders.matchAllQuery())
            .size(pageLimit)
            .sort("createdDate", SortOrder.DESC)
            .sort("url", SortOrder.ASC);

    var sr = new SearchRequest("books").source(sb);

    // query the first page
    var searchResponse = client.search(sr, RequestOptions.DEFAULT);

    printResponse(searchResponse, pageLimit, 0);

    // page: 1
    if (searchResponse.getHits().getHits().length > 0) {
      var hits = searchResponse.getHits().getHits();
      // get last sort result
      var lastDate = (Long) (hits[hits.length - 1].getSourceAsMap().get("createdDate"));
      var lastUrl = (String) (hits[hits.length - 1].getSourceAsMap().get("url"));

      sb.searchAfter(new String[]{lastDate.toString(), lastUrl});
      sr.source(sb);
      searchResponse = client.search(sr, RequestOptions.DEFAULT);
      printResponse(searchResponse, pageLimit, ++currentPage);

      // current is page=1 and try access page 3
      sb.size(2 * pageLimit); // try to access page=2 and page=3;
      sb.searchAfter(new String[]{lastDate.toString(), lastUrl});
      sr.source(sb);
      searchResponse = client.search(sr, RequestOptions.DEFAULT);
      hits = searchResponse.getHits().getHits();

      hits = Arrays.copyOfRange(hits, 5, hits.length - 1); // split the third page of hits
      for (SearchHit hit : hits) {
        var map = hit.getSourceAsMap();
        System.out.println("name=" + map.get("name"));
      }

      System.out.println("total: " + searchResponse.getHits().getTotalHits().value);

      var pages = (searchResponse.getHits().getTotalHits().value + pageLimit - 1) / pageLimit;
      System.out.println("pages: " + pages);
      System.out.println("page: " + 3);
      System.out.println("========================================");
    }
  }


  /**
   * 查找对应名字的book, 计算picCount的平均值.
   * 这里要用aggreate方法，不能使用search
   * https://stackoverflow.com/questions/62467520/retrieve-aggregations-using-spring-data-elasticsearch-reactive-template
   */
  @Test
  public void testAggregation() {
    var name = "我的";
    var query = new NativeSearchQueryBuilder()
        .withQuery(QueryBuilders.matchPhraseQuery("name", "我的").slop(1))

        /*
          max  最大值
          min  最小值
          sum  求和
          avg  平均值
         */
        .addAggregation(AggregationBuilders.avg("avg_pic_count").field("picCount"))
        .addAggregation(AggregationBuilders.max("max").field("picCount"))
        .addAggregation(AggregationBuilders.min("min").field("picCount"))
        .addAggregation(AggregationBuilders.sum("sum").field("picCount"))
//        .withMaxResults(0) //not ready in this version
//        .withSourceFilter(new FetchSourceFilterBuilder().build())
        .build();
    query.setMaxResults(0);

    template.aggregate(query, Book.class)
        .flatMap(aggregation -> Mono.just((ParsedSingleValueNumericMetricsAggregation) aggregation))
        .doOnNext(parsedAvg -> {
          System.out.println("name=" + name + ",  aggr picCount=" + parsedAvg.value() + ", aggr field=" + parsedAvg.getName());
        })
        .blockLast();
  }

  private void search(Query query) {
    template
        .search(query, Book.class)
        .doOnNext(
            searchHit -> {
              var book = searchHit.getContent();
              System.out.println("name=" + book.getName());
            })
        .blockLast();
  }

  private void printResponse(SearchResponse serverResponse, int pageLimit, int currentPage) {
    var hits = serverResponse.getHits().getHits();

    for (SearchHit hit : hits) {
      var map = hit.getSourceAsMap();
      System.out.println("name=" + map.get("name"));
    }

    System.out.println("total: " + serverResponse.getHits().getTotalHits().value);

    var pages = (serverResponse.getHits().getTotalHits().value + pageLimit - 1) / pageLimit;
    System.out.println("pages: " + pages);
    System.out.println("page: " + currentPage);
    System.out.println("========================================");
  }
}
