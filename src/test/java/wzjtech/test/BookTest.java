package wzjtech.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import wzjtech.test.spring.entity.Book;
import wzjtech.test.spring.repo.BookRep;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Set;

@SpringBootTest
public class BookTest {

  @Autowired
  BookRep bookRep;

  @Autowired
  ReactiveElasticsearchTemplate template;

  private Set<Book> getBooks() throws IOException {
    String path = "books.xml";
    boolean isFile = Paths.get(path).toFile().isFile();
    if (!isFile) {
      throw new IllegalArgumentException("File doesn't exist");
    }
    XmlMapper xmlMapper = new XmlMapper();
    File outputFile = new File(path);

    Set<Book> set = xmlMapper
        .readValue(outputFile, new TypeReference<Set<Book>>() {
        });
    return set;
  }

  @Test
  public void testImport() throws IOException {
    var books = getBooks();
    template.saveAll(books, IndexCoordinates.of("books")).blockLast();
  }


}
