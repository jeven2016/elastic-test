package wzjtech.test.spring.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@Builder
public class Person {
  @Id private String id;

  @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
  private String name;

  // 支持分词，全文检索,支持模糊、精确查询,不支持聚合,排序操作;
  // 不添加text会被设置成keyword类型
  // (不进行分词，直接索引,支持模糊、支持精确匹配，支持聚合、排序操作。
  // 存储邮箱号码、url、name、title，手机号码、主机名、状态码、邮政编码、标签、年龄、性别等数据)
  @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
  private String introduction;

  @Field(type = FieldType.Integer)
  private int age;
}
