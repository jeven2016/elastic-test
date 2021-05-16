package wzjtech.test.spring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.stereotype.Service;
import wzjtech.test.spring.entity.Person;
import wzjtech.test.spring.entity.SchoolDocument;

@Service
public class IndexOpService {
  //ReactiveElasticsearchTemplate lets you save, find and delete your domain objects and map those objects
  // to documents stored in Elasticsearch.
  @Autowired
  ReactiveElasticsearchTemplate template;

  //  The IndexOperations interface and the provided implementation which can be obtained from an
  //  ElasticsearchOperations instance - for example with a call to operations.indexOps(clazz)- give
  //  the user the ability to create indices, put mappings or store template and alias information
  //  in the Elasticsearch cluster. Details of the index that will be created can be set by using
  //  the @Setting annotation, refer to Index settings for further information.
  public ReactiveIndexOperations getIndexOps() {
    ReactiveIndexOperations indexOperations = template.indexOps(Person.class);
    return indexOperations;
  }

  public void createIndex() {
//    getIndexOps().
  }
}
