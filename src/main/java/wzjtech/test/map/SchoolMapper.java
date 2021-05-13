package wzjtech.test.map;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import reactor.core.publisher.Mono;
import wzjtech.test.dto.SchoolDto;
import wzjtech.test.spring.entity.SchoolDocument;

@Mapper
public interface SchoolMapper {
  SchoolMapper INSTANCE = Mappers.getMapper(SchoolMapper.class);

  SchoolDto toDto(SchoolDocument schoolDocument);

  SchoolDocument toDocument(SchoolDto dto);

  default Mono<SchoolDto> toDtoMono(Mono<SchoolDocument> mono) {
    return mono.map(this::toDto);
  }
}
