package tyler.server.dto.task;

public record TaskResponseDTO(
    Long id,
    int subtasks,
    String name,
    String description,
    String dueDate,
    byte xp,
    boolean done
) {}