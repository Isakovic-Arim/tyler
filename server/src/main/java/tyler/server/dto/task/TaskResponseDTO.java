package tyler.server.dto.task;

public record TaskResponseDTO(
    Long id,
    int subtasks,
    String name,
    String description,
    String dueDate,
    String deadline,
    byte xp,
    boolean done
) {}