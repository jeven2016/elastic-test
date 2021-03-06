package wzjtech.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.metrics.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import wzjtech.test.spring.entity.Book;
import wzjtech.test.spring.repo.BookRep;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;

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
    var criteria = new Criteria("name").is("??????").or("name").is("??????2");
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
                "query":"??????",
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

    // ??????????????????????????????????????????????????????"???"?????????????????????
    Query query = queryBuilder.withQuery(QueryBuilders.matchQuery("name", "????????????")).build();

    search(query);
  }

  /*
  ??????"????????????????????????"???????????????"???????????????????????????"??????????????????????????????????????????????????????????????????"?????? ?????? ??????"????????????????????????????????? match_phrase ???
    */
  @Test
  public void testPhraseQuery() {
    var query =
        new NativeSearchQueryBuilder()
            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????slop
            .withQuery(QueryBuilders.matchPhraseQuery("name", "?????????").slop(1))
            .build();

    search(query);
  }

  /*
    GET /books/_search
  {
    "query": {
      "bool": {
        "must": [
          { "match": { "name":   "?????????"        }}
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
            .withQuery(QueryBuilders.matchQuery("name", "?????????"))
            // do further query based on previous result
            .withFilter(QueryBuilders.rangeQuery("picCount").gte(1000))
            .build();
    query.setMaxResults(10);
    search(query);

    // or with bool query
    query =
        new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("name", "?????????")))
            .withFilter(QueryBuilders.rangeQuery("picCount").gte(1000))
            .build();
    query.setMaxResults(10);
    search(query);
  }

  /*
  ???????????????????????????
   */
  @Test
  public void testMultiMatch() {
    var query =
        new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.multiMatchQuery("?????????", "name", "url"))
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
   * ?????????????????????book, ??????picCount????????????.
   * ????????????aggreate?????????????????????search
   * https://stackoverflow.com/questions/62467520/retrieve-aggregations-using-spring-data-elasticsearch-reactive-template
   */
  @Test
  public void testAggregation() {
    var name = "??????";
    var query = new NativeSearchQueryBuilder()
        .withQuery(QueryBuilders.matchPhraseQuery("name", "??????").slop(1))

        /*
          max  ?????????
          min  ?????????
          sum  ??????
          avg  ?????????
          value_count  ???????????????????????????????????????hits.total.value????????????
         */
        .addAggregation(AggregationBuilders.avg("avg_pic_count").field("picCount"))
        .addAggregation(AggregationBuilders.max("max").field("picCount"))
        .addAggregation(AggregationBuilders.min("min").field("picCount"))
        .addAggregation(AggregationBuilders.sum("sum").field("picCount"))
        .addAggregation(AggregationBuilders.count("name_count").field("name.keyword")) //value_count??? ???????????????????????????????????????
        .addAggregation(AggregationBuilders.cardinality("cardinality_field").field("name.keyword"))//distinct, ??????????????????????????????????????????
        .withMaxResults(0) //not ready in this version
//        .withSourceFilter(new FetchSourceFilterBuilder().build())//?????????????????????????????????
        .build();

    template.aggregate(query, Book.class)
        .doOnNext(aggregation -> {
          if (aggregation instanceof ParsedSingleValueNumericMetricsAggregation parsedAggregation) {
            System.out.println("name=" + name + ",  aggr picCount=" + parsedAggregation.value() + ", aggr field=" + parsedAggregation.getName());
          }

          if (aggregation instanceof ParsedValueCount parsedValueCount) {
            System.out.println("name=" + name + ", aggr value_count=" + parsedValueCount.value() + ", aggr field=" + parsedValueCount.getName());
          }

          if (aggregation instanceof ParsedCardinality parsedCardinality) {
            System.out.println("name=" + name + ", aggr cardinality=" + parsedCardinality.value() + ", aggr field=" + parsedCardinality.getName());
          }
        })
        .blockLast();
  }


  /**
   * Stats ??????
   * ?????????????????????max, min, count, avg?????? ????????????string??????
   * <p>
   * extend Stats????????? ??????????????????????????????
   */
  @Test
  public void testStats() {
    var query = new NativeSearchQueryBuilder()
        .withQuery(QueryBuilders.matchAllQuery())
        .addAggregation(AggregationBuilders.stats("stats_name").field("picCount"))
//        .addAggregation(AggregationBuilders.extendedStats("extend_stats_name").field("picCount"))//?????????????????????????????????????????????????????????
        .build();

    template.aggregate(query, Book.class)
        .doOnNext(aggregation -> {
          System.out.println("aggrName=" + aggregation.getName());
          var aggr = (ParsedStats) aggregation;
          System.out.println("avg=" + aggr.getAvg());
          System.out.println("max=" + aggr.getMax());
          System.out.println("min=" + aggr.getMin());
          System.out.println("sum=" + aggr.getSum());
          System.out.println("count=" + aggr.getCount());

          if (aggr instanceof ParsedExtendedStats parsedExtendedStats) {
            System.out.println("sumOfSquares=" + parsedExtendedStats.getSumOfSquares());
            System.out.println("Variance=" + parsedExtendedStats.getVarianceAsString());
            System.out.println("StdDeviation=" + parsedExtendedStats.getStdDeviationAsString());
            System.out.println("StdDeviationPopulation=" + parsedExtendedStats.getStdDeviationPopulationAsString());
            System.out.println("StdDeviationBound_lower=" + parsedExtendedStats.getStdDeviationBoundAsString(ExtendedStats.Bounds.LOWER));
          }
        })
        .then().block();
  }

  /**
   * ????????????????????????????????????
   * <p>
   * ??????????????????:
   * {latency: 200, ....}
   * {latency: 400, ....}
   * {latency: 500, ....}
   * ....
   * <p>
   * ????????????50%????????????latency???????????????????????? 90%?????????????????????
   */
  @Test
  public void testPercentage() {
    System.out.println("========================================");
    var query = new NativeSearchQueryBuilder()
        .withQuery(QueryBuilders.matchPhraseQuery("name", "??????"))
        .addAggregation(AggregationBuilders.percentiles("percentage_picCount").field("picCount").percentiles(50, 75, 99))
        .addAggregation(AggregationBuilders.percentileRanks("per_ranks", new double[]{50, 99}).field("picCount"))
        .addAggregation(AggregationBuilders.avg("avg_picCount").field("picCount"))
        .build();

     /*
      ?????????
      50.0% : 1193.5
      75.0% : 3716.0
      99.0% : 5345.0

      ??????50%???????????????????????????1193.5??? 75%??????3716?????????
      */
    template.aggregate(query, Book.class)
        .doOnNext((aggregation) -> {

          if (aggregation instanceof ParsedPercentiles percentilesAggr) {
            percentilesAggr.iterator().forEachRemaining(percentile -> {
              System.out.println(percentile.getPercent() + "% : " + percentile.getValue());
            });
          }

          if (aggregation instanceof ParsedTDigestPercentileRanks ranks) {
            ranks.iterator().forEachRemaining(percentile -> {
              System.out.println("Rank=> " + percentile.getPercent() + "% : " + percentile.getValue());
            });
          }

          if (aggregation instanceof ParsedAvg parsedAvg) {
            System.out.println(parsedAvg.getName() + "=" + parsedAvg.getValue());
          }
        }).then().block();
  }

  /**
   * ?????????????????????, ??????picCount???????????????500??????????????? ?????????10000??????????????????
   */
  @Test
  public void testPerRank() {
    System.out.println("========================================");
    var query = new NativeSearchQueryBuilder()
        .withQuery(QueryBuilders.matchAllQuery())
        .addAggregation(AggregationBuilders.percentileRanks("per_ranks", new double[]{500, 10000}).field("picCount"))
        .addAggregation(AggregationBuilders.avg("avg_picCount").field("picCount"))
        .build();

    template.aggregate(query, Book.class)
        .doOnNext((aggregation) -> {
          if (aggregation instanceof ParsedTDigestPercentileRanks ranks) {
            ranks.iterator().forEachRemaining(percentile -> {
              System.out.println("Rank=> " + percentile.getValue() + " : " + percentile.getPercent() + "%");
            });
          }
        }).then().block();
  }

  /**
   * ?????????????????????top5?????????picCount???
   * <p>
   * ?????????????????????status???????????????????????? ?????????????????????????????????????????????sub aggregation?????????.
   * ?????????
   * key=COMPLETED, count=892
   * key=FAILED, count=30
   * key=NEW_ADDED, count=23
   */
  @Test
  public void testGroupingByTerm_hasValueCount() {
    System.out.println("========================================");
    var query = new NativeSearchQueryBuilder()
        .addAggregation(AggregationBuilders.terms("status_term")
            .field("status")
            .subAggregation(AggregationBuilders.count("field_count").field("name.keyword")))
        .build();

    template.aggregate(query, Book.class)
        .doOnNext(aggregation -> {
          var aggr = (ParsedStringTerms) aggregation;
          aggr.getBuckets().forEach(bucket -> {
            var valueCount = (ParsedValueCount) bucket.getAggregations().asMap().get("field_count");
            System.out.println("key=" + bucket.getKeyAsString() + ", count=" + valueCount.getValue());
          });

        }).then().block();
  }

  /**
   * Term????????????
   */
  @Test
  public void testGroupingByTerm() {
    System.out.println("========================================");
    var query = new NativeSearchQueryBuilder()
        .addAggregation(AggregationBuilders.terms("status_term").field("status"))
        .build();

    template.aggregate(query, Book.class)
        .doOnNext(aggregation -> {
          var aggr = (ParsedStringTerms) aggregation;
          aggr.getBuckets().forEach(bucket -> {
            System.out.println("key=" + bucket.getKeyAsString() + ", doc count=" + bucket.getDocCount());
          });
        }).then().block();
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


  private void printJson(String key, Object obj) {
    try {
      System.out.println("key=" + key + " " + objectMapper.writeValueAsString(obj));
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }
}
