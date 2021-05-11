package wzjtech.test.spring.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import wzjtech.test.spring.entity.SchoolDocument;
import wzjtech.test.spring.repo.SchoolRep;

@Service
public class SpringSchoolService {
  private SchoolRep schoolRep;

  public SpringSchoolService(SchoolRep schoolRep) {
    this.schoolRep = schoolRep;
  }

  public Mono<SchoolDocument> findById(String id) {
    return schoolRep.findById(id);
  }

  public Mono<SchoolDocument> save(SchoolDocument schoolDocument){
    return schoolRep.save(schoolDocument);
  }
}
