package wzjtech.test.lowlevel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Random;

@Getter
@Setter
@Builder
@NoArgsConstructor
class Student {
  String name;
  int age;
  String desc;
}

@Service
@Slf4j
public class HighLevelClientService {
  private final RestHighLevelClient client;
  private final ObjectMapper objectMapper;

  private final static Random random = new Random();

  public HighLevelClientService(RestHighLevelClient client,
                                ObjectMapper objectMapper) {
    this.client = client;
    this.objectMapper = objectMapper;
  }

  public void buildIndexAsync(String indexName, String name) throws JsonProcessingException {
    var student = Student.builder().name(name).age(random.nextInt(100)).desc(name + ":" + 18).build();
    var studentJsonStr = objectMapper.writeValueAsString(student);

    //自动生成ID, 每次都会创建student
    var indexReq = new IndexRequest(indexName)
        .source(studentJsonStr, XContentType.JSON) //or set map
        //optional
        .timeout(TimeValue.timeValueSeconds(10))
        .setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
//        .opType(DocWriteRequest.OpType.CREATE); //mapping type name [_create] can't start with '_' unless it is called [_doc]]

    //async
    client.indexAsync(indexReq, RequestOptions.DEFAULT, new ActionListener<IndexResponse>() {
      @Override
      public void onResponse(IndexResponse indexResponse) {
        log.warn("Index succeed, {}", indexResponse);
      }

      @Override
      public void onFailure(Exception e) {
        log.warn("Failed to index", e);
      }
    });

    //sync
//    try {
//      client.index(indexReq, RequestOptions.DEFAULT);
//    } catch (IOException e) {
//      e.printStackTrace();
//    }

  }
}
