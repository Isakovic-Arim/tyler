package tyler.server.mapper;

import org.mapstruct.Mapper;
import tyler.server.dto.priority.PriorityResponseDto;
import tyler.server.entity.Priority;

@Mapper(componentModel = "spring")
public abstract class PriorityMapper {
    public abstract PriorityResponseDto toResponseDto(Priority priority);
}