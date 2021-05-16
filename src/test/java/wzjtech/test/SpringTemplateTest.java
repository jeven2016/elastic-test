package wzjtech.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import reactor.core.publisher.Mono;
import wzjtech.test.spring.entity.Person;

@SpringBootTest
public class SpringTemplateTest {

  @Autowired
  ReactiveElasticsearchTemplate template;

  @Autowired
  ObjectMapper mapper;

  private static class SchoolClass {

  }

  /**
   * create mapping
   */
  @Test
  public void testCreateMapping() {
    ReactiveIndexOperations operations = template.indexOps(Person.class);
    operations.createMapping().flatMap(doc -> {
      try {
        return Mono.just(mapper.writeValueAsString(doc));
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
      return Mono.empty();
    }).doOnNext(System.out::println).block();

  }

  @Test
  public void upsertPerson() {
    var person = Person.builder().name("wzj").age(40).id("wzj")
        .introduction("他是谁呢，谁知道啊，反正我不清楚呢。")
        .build();

    //指定索引名为person
    template.save(person, IndexCoordinates.of("person"))
        .flatMap(this::print).block();
  }

  @Test
  public void deletePerson() {
    template.delete("wzj", IndexCoordinates.of("person"))
        .doOnNext(this::print).block();
  }

  private Mono<String> print(Object obj) {
    try {
      return Mono.just(mapper.writeValueAsString(obj));
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return Mono.empty();
  }
}
