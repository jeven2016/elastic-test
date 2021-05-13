package wzjtech.test.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import wzjtech.test.dto.SchoolDto;
import wzjtech.test.map.SchoolMapper;
import wzjtech.test.spring.entity.SchoolDocument;
import wzjtech.test.spring.service.SpringSchoolService;

@RestController
@RequestMapping("spring/school")
public class SpringSchoolRest {
  private SpringSchoolService service;

  public SpringSchoolRest(SpringSchoolService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<SchoolDocument> create(@RequestBody SchoolDto dto) {
    return service.save(SchoolMapper.INSTANCE.toDocument(dto));
  }

  @GetMapping("{id}")
  public Mono<SchoolDto> findById(@PathVariable String id) {
    return SchoolMapper.INSTANCE.toDtoMono(service.findById(id));
  }

  @DeleteMapping("{id}")
  public Mono<Void> deleteById(@PathVariable String id) {
    return service.delete(id);
  }
}
