package wzjtech.test.spring.entity;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "books")
@Getter
@Setter
// @TypeAlias("BookType") or class name used
public class Book {
  @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
  private String name;

  @Field(type = FieldType.Keyword)
  private String url;

  @Field(type = FieldType.Integer)
  private int picPageCount;

  @Field(type = FieldType.Integer)
  private int picCount;

  @Field(type = FieldType.Keyword)
  private String status;

  List<String> pictures;
}
