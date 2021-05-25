package wzjtech.test.spring.entity;

import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Document(indexName = "page_view")
@Builder
public class PageView implements Persistable<String> {
  @Id
  private String id;

  @Field(type = FieldType.Keyword)
  private String pageUrl;

  @Field(type = FieldType.Auto)
  private Long count;

  @Field(type = FieldType.Keyword)
  private String userName;

  @CreatedDate
  @Field(type = FieldType.Date)
  private Instant createdDate;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return id == null || createdDate == null;
  }
}
