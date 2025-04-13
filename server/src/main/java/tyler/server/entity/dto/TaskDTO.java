package tyler.server.entity.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

import static tyler.server.Constants.*;

public record TaskDTO(
    Long id,

    @NotBlank(message = "Task name cannot be blank")
    @Size(min = 3, max = MAX_NAME_LENGTH, message = "Task name must be between 3 and 100 characters")
    String name,

    @Size(max = MAX_DESCRIPTION_LENGTH, message = "Description cannot exceed 500 characters")
    String description,

    @FutureOrPresent(message = "Due date must be in the future or present")
    LocalDate dueDate,

    @NotNull(message = "Deadline is required")
    @FutureOrPresent(message = "Deadline must be in the future or present")
    LocalDate deadline,

    @Min(value = 1, message = "XP must be at least 1")
    @Max(value = MAX_XP, message = "XP cannot exceed 100")
    byte xp,

    boolean done
) {}