package wzjtech.test.spring.entity;

import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

// index can be treated as database
@Document(indexName = "student")
@TypeAlias("school")
// @Getter
// @Setter
public class SchoolDocument {
  @Id private String id;

  @Field(type = FieldType.Text)
  private String name;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getIntroduction() {
    return introduction;
  }

  public void setIntroduction(String introduction) {
    this.introduction = introduction;
  }

  @Field(type = FieldType.Text)
  private String introduction;
}
