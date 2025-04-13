package tyler.server.entity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tyler.server.entity.Task;
import tyler.server.entity.dto.TaskDTO;


@Mapper(componentModel = "spring")
public interface TaskMapper {
    TaskMapper INSTANCE = Mappers.getMapper(TaskMapper.class);

    Task DtoToTask(TaskDTO taskDTO);
}
