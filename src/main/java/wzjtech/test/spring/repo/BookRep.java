package wzjtech.test.spring.repo;

import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import reactor.core.publisher.Mono;
import wzjtech.test.spring.entity.Book;

//for more details, refer to SimpleElasticsearchRepository
public interface BookRep extends ReactiveElasticsearchRepository<Book, String> {

  @Override
  Mono<Book> findById(String s);
}
