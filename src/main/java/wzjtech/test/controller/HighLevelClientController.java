package wzjtech.test.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import wzjtech.test.raw.HighLevelClientService;

@RestController
@RequestMapping("hlTest")
public class HighLevelClientController {
  private final HighLevelClientService service;

  public HighLevelClientController(HighLevelClientService service) {
    this.service = service;
  }

  @GetMapping("create/async/{name}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public String buildIndexAsync(@PathVariable String name) throws JsonProcessingException {
    // index name must be lowercase
    service.buildIndexAsync("student", name);
    return "ok";
  }

  @GetMapping("source/{indexName}/{id}")
  public Map<String, Object> findStudent(@PathVariable String indexName, @PathVariable String id) {
    return service.findByIdWithSource(indexName, id);
  }

  /** No desc retrieved */
  @GetMapping("source/{indexName}/{id}/excludeDesc")
  public Map<String, Object> findStudentExcludeDescField(
      @PathVariable String indexName, @PathVariable String id) {
    return service.findByIdExcludeFields(indexName, id);
  }

  @DeleteMapping("source/{indexName}/{id}")
  public void deleteById(@PathVariable String indexName, @PathVariable String id)
      throws IOException {
    service.deleteById(indexName, id);
  }
}
