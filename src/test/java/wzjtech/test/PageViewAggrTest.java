package wzjtech.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.range.ParsedDateRange;
import org.elasticsearch.search.aggregations.bucket.range.ParsedRange;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.metrics.ParsedMax;
import org.elasticsearch.search.aggregations.metrics.ParsedTopHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import wzjtech.test.spring.entity.PageView;

import java.util.Random;
import java.util.stream.IntStream;

@SpringBootTest
public class PageViewAggrTest {

  @Autowired
  ReactiveElasticsearchTemplate template;

  @Autowired
  ObjectMapper objectMapper;

  Random random = new Random();
  Random random2 = new Random();

  private String[] urls = {"http://hello.com/page1", "http://hello.com/page2",
      "http://hello.com/page3", "http://hello.com/page4", "http://hello.com/page5", "http://hello.com/page6"};

  private String[] names = {"Jack", "Bob", "Wang", "Robert", "William", "Cart","帅帅", "壮大胖", "蜗牛", "不喜欢夏天", "秋天再过冬"};

  @Test
  public void importPageViewRecords() {
    //generate 10000000
    IntStream.range(0, 10000000).forEach(value -> {
      var randomUrlIndex = ((value * random2.nextInt(50)) % urls.length);
      var randomNameIndex = ((value * random2.nextInt(50)) % names.length);

      System.out.println(randomUrlIndex + ", " + randomNameIndex);
      var url = urls[randomUrlIndex];
      var userName = names[randomNameIndex];

      var pageView = PageView.builder()
          .pageUrl(url).count((long) random.nextInt(20000)).userName(userName).build();

      template.save(pageView).then().block();
    });
  }

  /**
   * 根据url去分组，统计每个url访问的总和
   * 利用默认的的doc_count去计算数目， 不需要添加子aggregation
   * <p>
   * <p>
   * Note: 执行分组时，spring默认添加一个"order":[{"_count":"desc"},{"_key":"asc"}]}, 即以文档的个数进行排序，
   * 因此返回结果中带了一个count字段，所以就不需要下面的sub aggregation了
   * {"key" : "COMPLETED","doc_count" : 892}
   */
  @Test
  public void testGroupingByTerm() {
    System.out.println("========================================");
    var query = new NativeSearchQueryBuilder()
        .addAggregation(AggregationBuilders.terms("url_field").field("pageUrl.keyword"))
        .build();

    template.aggregate(query, PageView.class)
        .doOnNext(aggregation -> {
          var aggr = (ParsedStringTerms) aggregation;
          aggr.getBuckets().forEach(bucket -> {
            System.out.println("key=" + bucket.getKeyAsString() + ", doc count=" + bucket.getDocCount());
          });
        }).then().block();
  }


  /**
   * 将所有的文档按照count从大到小排序，取top5且只包含id和count字段的文档
   */
  @Test
  public void testTopHits() {
    var query = new NativeSearchQueryBuilder()
        .withQuery(QueryBuilders.matchAllQuery())
        .addAggregation(AggregationBuilders.topHits("hits_name")
            .size(5)
            .sort("count", SortOrder.DESC)
            .fetchSource(new String[]{"count", "id"}, null))
        .build();

    template.aggregate(query, PageView.class)
        .doOnNext(aggregation -> {
          var aggr = (ParsedTopHits) aggregation;
          for (var hit : aggr.getHits().getHits()) {
            var count = hit.getSourceAsMap().get("count");
            System.out.println(aggregation.getName() + " top: " + count);
          }
        }).then().block();
  }

  /**
   * 先将所有文档以userName分组，并且只取一个分组。
   * 将查询所得的文档按照count从大到小排序，取top5且只包含id和count字段的文档
   */
  @Test
  public void testGroupingTopHits() {
    var query = new NativeSearchQueryBuilder()
        .addAggregation(AggregationBuilders.terms("term_field").field("userName.keyword").size(1)
            .subAggregation(AggregationBuilders.topHits("hits_name")
                .size(5)
                .sort("count", SortOrder.DESC)
                .fetchSource(new String[]{"count", "id"}, null)))
        .build();
//      同理: 先查找Jack在分组
//    var query = new NativeSearchQueryBuilder()
//        .withQuery(QueryBuilders.termQuery("userName.keyword", "Jack"))
//        .addAggregation(AggregationBuilders.terms("term_field").field("userName.keyword")
//            .subAggregation(AggregationBuilders.topHits("hits_name")
//                .size(5)
//                .sort("count", SortOrder.DESC)
//                .fetchSource(new String[]{"count", "id"}, null)))
//        .build();

    template.aggregate(query, PageView.class)
        .doOnNext(aggregation -> {
          var aggr = (ParsedStringTerms) aggregation;
          for (var bucket : aggr.getBuckets()) {
            bucket.getAggregations().asList().forEach(hit -> {
              var parsedHit = (ParsedTopHits) hit;
              for (var searchHit : parsedHit.getHits().getHits()) {
                var count = searchHit.getSourceAsMap().get("count");
                System.out.println(aggregation.getName() + " top: " + count);
              }
            });
          }
        }).then().block();
  }

  /**
   * Term aggregation
   * 为一个桶，并计算每个桶内文档个数。默认返回顺序是按照文档个数多少排序
   * 把满足相关特性的文档分到一个桶里，即桶分，输出结果往往是一个个包含多个文档的桶（一个桶就是一个group
   * <p>
   * 以username进行分组，取5条， 其中数据以count排序，且文档数最少有500的分组才符合。 默认以具有的文档数目从高到底排序
   */
  @Test
  public void testBucket() {
    var query = new NativeSearchQueryBuilder()
        .addAggregation(AggregationBuilders.terms("term_name")
            .field("userName.keyword")
            .size(2)
            .minDocCount(500))
        .build();

    template.aggregate(query, PageView.class)
        .doOnNext(aggregation -> {
          var aggr = (ParsedStringTerms) aggregation;
          aggr.getBuckets().forEach(bucket -> {
            System.out.println("key=" + bucket.getKeyAsString() + ", count=" + bucket.getDocCount());
          });
        }).then().block();
  }

  /**
   * 非聚合：
   * <p>
   * 找出count最大的文档并返回内容
   */
  @Test
  public void testFindDocWithMaxCount() {
    var query = new NativeSearchQueryBuilder()
        .withQuery(QueryBuilders.matchAllQuery())
        .withSort(SortBuilders.fieldSort("count").order(SortOrder.DESC))
        .withMaxResults(1)
        .build();

    template.search(query, PageView.class)
        .doOnNext(aggregation -> {
          System.out.println(aggregation);
        }).then().block();

  }

  /**
   * 根据count值进行范围聚合统计，并返回符合的文档数目
   * <p>
   * 统计大于2000的文档数已经文档数在5000和10000范围之类的数目doc_count
   * <p>
   * 返回：
   * "buckets" : [
   * {
   * "key" : "*-2000.0",
   * "to" : 2000.0,
   * "doc_count" : 15766
   * },
   * {
   * "key" : "1000.0-10000.0",
   * "from" : 1000.0,
   * "to" : 10000.0,
   * "doc_count" : 70437
   * }
   * ]
   */
  @Test
  public void testRange() {
    var query = new NativeSearchQueryBuilder()
        .addAggregation(AggregationBuilders.range("range_name")
                .field("count")
                .addRange(5000, 10000)
                .addUnboundedTo(2000)
//            .addUnboundedFrom(1000)
                .subAggregation(AggregationBuilders.max("max_count").field("count"))
        )
//        .withMaxResults(5)
        .build();

    template.aggregate(query, PageView.class)
        .doOnNext(aggregation -> {
          var aggr = (ParsedRange) aggregation;
          aggr.getBuckets().forEach(bucket -> {
            var parsedMax = (ParsedMax) bucket.getAggregations().getAsMap().get("max_count");
            System.out.println("key=" + bucket.getKeyAsString() + ", count=" + bucket.getDocCount()
                + ", max_count=" + parsedMax.getValue());
          });

        }).then().block();

  }

  /**
   * shortcut: tt
   * <p>
   * 查询某个时间段里的文档数量，可以只指定from-to中的一个。
   */
  @Test
  public void testDateRange() {
    var query = new NativeSearchQueryBuilder()
        .addAggregation(AggregationBuilders
            .dateRange("data_field")
            .field("createdDate")
            .format("yyyy-MM-dd")
            .addRange("2021-05-27", "2021-05-28")
        ).build();

    template.aggregate(query, PageView.class)
        .doOnNext(aggregation -> {
          var aggr = (ParsedDateRange) aggregation;
          aggr.getBuckets().forEach(bucket -> {
            var count = bucket.getDocCount();
            System.out.println("key=" + bucket.getKeyAsString() + ", count=" + count);
          });
          System.out.println(aggregation);
        }).then().block();


  }

}
