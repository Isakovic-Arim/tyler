package tyler.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import tyler.server.entity.Task;
import tyler.server.dto.task.TaskRequestDTO;
import tyler.server.dto.task.TaskResponseDTO;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT
)
public abstract class TaskMapper {
    @Mapping(target = "priority", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "subtasks", ignore = true)
    @Mapping(target = "done", constant = "false")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    public abstract Task toTask(TaskRequestDTO taskRequestDTO);

    @Mapping(target = "subtasks", expression = "java(task.getSubtasks().size())")
    @Mapping(target = "dueDate", defaultExpression = "java(\"\")")
    @Mapping(target = "xp", source = "priority.xp")
    @Mapping(target = "description")
    @Mapping(target = "name")
    @Mapping(target = "id", defaultExpression = "java(0L)")
    @Mapping(target = "parentId", expression = "java(task.getParent() != null ? task.getParent().getId() : null)")
    public abstract TaskResponseDTO toResponseDto(Task task);
}