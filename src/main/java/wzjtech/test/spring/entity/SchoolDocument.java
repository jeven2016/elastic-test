package wzjtech.test.spring.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

// index can be treated as database
@Document(indexName = "school")
@TypeAlias("school")
@Getter
@Setter
public class SchoolDocument {
  @Id private String id;

  @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
  private String name;

  @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
  private String introduction;
}
