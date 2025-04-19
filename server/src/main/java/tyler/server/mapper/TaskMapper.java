package tyler.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tyler.server.entity.Task;
import tyler.server.dto.task.TaskRequestDTO;

@Mapper(componentModel = "spring", uses = {PriorityMapper.class})
public abstract class TaskMapper {
    @Mapping(source = "priorityId", target = "priority", qualifiedByName = "idToPriority")
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "subtasks", ignore = true)
    @Mapping(target = "done", constant = "false")
    @Mapping(target = "id", ignore = true)
    public abstract Task RequestDtoToTask(TaskRequestDTO taskRequestDTO);
}