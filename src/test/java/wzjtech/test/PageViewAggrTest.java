package wzjtech.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
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

  private String[] names = {"Jack", "Bob", "Wang", "Robert", "William", "Cart"};

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
          .pageUrl(url).count(random.nextLong()).userName(userName).build();

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

}
