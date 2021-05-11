package wzjtech.test.raw;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.springframework.stereotype.Service;

@Getter
@Setter
@Builder
class Student {
  String name;
  int age;
  String desc;
}

/**
 * Interact with elasticsearch by the raw driver
 */
@Service
@Slf4j
public class HighLevelClientService {
  private final RestHighLevelClient client;
  private final ObjectMapper objectMapper;

  private static final Random random = new Random();

  public HighLevelClientService(RestHighLevelClient client, ObjectMapper objectMapper) {
    this.client = client;
    this.objectMapper = objectMapper;
  }

  /** index a document */
  public void buildIndexAsync(String indexName, String name) throws JsonProcessingException {
    var age = random.nextInt(100);
    var student = Student.builder().name(name).age(age).desc(name + ":" + age).build();
    var studentJsonStr = objectMapper.writeValueAsString(student);

    // 自动生成ID, 每次都会创建student
    var indexReq =
        new IndexRequest(indexName)
            .source(studentJsonStr, XContentType.JSON) // or set map
            // optional
            .timeout(TimeValue.timeValueSeconds(10))
            .setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
    //        .opType(DocWriteRequest.OpType.CREATE); //mapping type name [_create] can't start with
    // '_' unless it is called [_doc]]

    // async
    client.indexAsync(
        indexReq,
        RequestOptions.DEFAULT,
        new ActionListener<IndexResponse>() {
          @Override
          public void onResponse(IndexResponse indexResponse) {
            var id = indexResponse.getId();
            var result = indexResponse.getResult();
            log.info("Index operation succeed, id={}, result={}", id, result.name());
            var shardInfo = indexResponse.getShardInfo();
            log.info("All succeed: {}", shardInfo.getSuccessful() == shardInfo.getTotal());
          }

          @Override
          public void onFailure(Exception e) {
            log.warn("Failed to index", e);
          }
        });

    // sync
    //    try {
    //      client.index(indexReq, RequestOptions.DEFAULT);
    //    } catch (IOException e) {
    //      e.printStackTrace();
    //    }

  }

  public Map<String, Object> findByIdWithSource(String indexName, String id) {
    var getRequest =
        new GetRequest()
            .id(id)
            .index(indexName)
            .fetchSourceContext(FetchSourceContext.FETCH_SOURCE);

    try {
      var response = client.get(getRequest, RequestOptions.DEFAULT);
      return response.getSourceAsMap();
    } catch (IOException e) {
      log.warn("An IOException occurs", e);
    }
    return null;
  }

  public Map<String, Object> findByIdExcludeFields(String indexName, String id) {
    // exclude desc field
    FetchSourceContext sourceContext =
        new FetchSourceContext(true, Strings.EMPTY_ARRAY, new String[] {"desc"});

    var getRequest = new GetRequest().id(id).index(indexName).fetchSourceContext(sourceContext);

    try {
      var response = client.get(getRequest, RequestOptions.DEFAULT);
      return response.getSourceAsMap();
    } catch (IOException e) {
      log.warn("An IOException occurs", e);
    }
    return null;
  }
}
