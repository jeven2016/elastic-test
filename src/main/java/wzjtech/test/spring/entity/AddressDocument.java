package wzjtech.test.spring.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "student")
public class AddressDocument {
  @Id private String id;

  @Field(type = FieldType.Text)
  private String area;

  @Field(type = FieldType.Text)
  private String address;
}
