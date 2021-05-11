package wzjtech.test.spring.entity;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

// index can be treated as database
@Document(indexName = "student")
@TypeAlias("school")
@Getter
@Setter
public class SchoolDocument implements Serializable {
  @Id private String id;

  @Field(type = FieldType.Text)
  private String name;

  @Field(type = FieldType.Text)
  private String introduction;
}
