package wzjtech.test.spring.repo;

import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import reactor.core.publisher.Mono;
import wzjtech.test.spring.entity.SchoolDocument;

public interface SchoolRep extends ReactiveElasticsearchRepository<SchoolDocument, String> {

  @Override
  Mono<SchoolDocument> findById(String s);
}
