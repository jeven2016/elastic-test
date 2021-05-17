package wzjtech.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Set;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import wzjtech.test.spring.entity.Book;
import wzjtech.test.spring.repo.BookRep;

@SpringBootTest
public class BookTest {

  @Autowired BookRep bookRep;

  @Autowired ReactiveElasticsearchTemplate template;

  private Set<Book> getBooks() throws IOException {
    String path = "books.xml";
    boolean isFile = Paths.get(path).toFile().isFile();
    if (!isFile) {
      throw new IllegalArgumentException("File doesn't exist");
    }
    XmlMapper xmlMapper = new XmlMapper();
    File outputFile = new File(path);

    Set<Book> set = xmlMapper.readValue(outputFile, new TypeReference<Set<Book>>() {});
    return set;
  }

  /**
   * Import some data
   *
   * @throws IOException
   */
  @Test
  public void testImport() throws IOException {
    var books = getBooks();
    template.saveAll(books, IndexCoordinates.of("books")).blockLast();
  }

  // with CriteriaQuery
  @Test
  public void searchByName() {
    var criteria = new Criteria("name").is("狂热").or("name").is("狂热2");
    var query = new CriteriaQuery(criteria);
    template
        .search(query, Book.class)
        .doOnNext(
            searchHit -> {
              var book = searchHit.getContent();
              System.out.println("name=" + book.getName());
            })
        .blockLast();
  }

  @Test
  public void searchByName_nativeQuery() {
    /*
      {
       "from":0,
       "size":10000,
       "query":{
          "match":{
             "name":{
                "query":"狂热",
                "operator":"OR",
                "prefix_length":0,
                "max_expansions":50,
                "fuzzy_transpositions":true,
                "lenient":false,
                "zero_terms_query":"NONE",
                "auto_generate_synonyms_phrase_query":true,
                "boost":1.0
             }
          }
       },
       "version":true
    }
         */
    var queryBuilder = new NativeSearchQueryBuilder();

    // 会进行分词查找，比如“女业务”会匹配"女"和”业务“记录
    Query query = queryBuilder.withQuery(QueryBuilders.matchQuery("name", "女业务员")).build();

    search(query);
  }

  /*
  比如"我的宝马多少马力"，一个文档"我的保时捷马力不错"也会被搜索出来，那么想要精确匹配所有同时包含"宝马 多少 马力"的文档怎么做？就要使用 match_phrase 了
    */
  @Test
  public void testPhraseQuery() {
    var query =
        new NativeSearchQueryBuilder()
            // 完全匹配可能比较严，我们会希望有个可调节因子，少匹配一个也满足，那就需要使用到slop
            .withQuery(QueryBuilders.matchPhraseQuery("name", "女业务").slop(1))
            .build();

    search(query);
  }

  /*
  多个字段匹配一个值
   */
  @Test
  public void testMultiMatch() {
    var query =
        new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.multiMatchQuery("女业务", "name", "url"))
            .build();
    search(query);
  }

  /*
    it works for keyword but seems not working for analyzed text
   */
  @Test
  public void testWildcardQuery() {
    var query =
        new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.wildcardQuery("url", "*OAjnAL*"))//? means only one char
            .build();

    search(query);
  }

  private void search(Query query) {
    template
        .search(query, Book.class)
        .doOnNext(
            searchHit -> {
              var book = searchHit.getContent();
              System.out.println("name=" + book.getName());
            })
        .blockLast();
  }
}
