package tyler.server.dto.task;

public record TaskResponseDTO(
    Long id,
    Long parentId,
    int subtasks,
    String name,
    String description,
    String dueDate,
    String deadline,
    byte remainingXp,
    boolean done
) {}