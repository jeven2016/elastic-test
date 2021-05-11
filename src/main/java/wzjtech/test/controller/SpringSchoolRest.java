package wzjtech.test.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import wzjtech.test.spring.entity.SchoolDocument;
import wzjtech.test.spring.service.SpringSchoolService;

@RestController
@RequestMapping("spring/school")
public class SpringSchoolRest {
  private SpringSchoolService service;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<SchoolDocument> create(@RequestBody SchoolDocument schoolDocument) {
    return service.save(schoolDocument);
  }

  @GetMapping("{id}")
  public Mono<SchoolDocument> findById(@PathVariable String id) {
    return service.findById(id);
  }
}
