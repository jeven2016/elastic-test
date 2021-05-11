package wzjtech.test.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import wzjtech.test.lowlevel.HighLevelClientService;

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
    //index name must be lowercase
    service.buildIndexAsync("student", name);
    return "ok";
  }
}
