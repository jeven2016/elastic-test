package wzjtech.test.spring.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;
import java.util.List;

@Document(indexName = "books")
@Getter
@Setter
// @TypeAlias("BookType") or class name used
public class Book implements Persistable<String> {
  @Id
  private String id;

  //使用@Field，使name字段可以分词查询， 比如“女业务”会被分割成“女”和业务，会搜出两种结果。如果需要完整匹配则需要match_phrase.
  //另一种方式，在这个字段上再加一个keyword,设置成not-analyzed field，即不分词匹配。使用时，需要以name.keyword名称查询
  // NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
  //  .withQuery(matchQuery("name.keyword", "Second Article About Elasticsearch"))
  //  .build()
//  @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
  @MultiField(
      mainField = @Field(type = FieldType.Text, fielddata = true),
      otherFields = {
          @InnerField(suffix = "keyword", type = FieldType.Keyword)
      }
  )
  private String name;

  @Field(type = FieldType.Keyword)
  private String url;

  @Field(type = FieldType.Integer)
  private int picPageCount;

  @Field(type = FieldType.Integer)
  private int picCount;

  //completed, failed
  @Field(type = FieldType.Keyword)
  private String status;

  List<String> pictures;

  /*
   * https://stackoverflow.com/questions/62581249/spring-data-elasticsearch-no-converter-found-capable-of-converting-from-type
   *  According to Spring Data Elasticserach documentation, you need to annotate an Instant property with @Field:
   */
  @CreatedDate
  //  @Field(type = FieldType.Date, format = DateFormat.custom, pattern =
  // "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
  @Field(type = FieldType.Date)
  private Instant createdDate;

  @Override
  public boolean isNew() {
    return id == null || createdDate == null;
  }
}
