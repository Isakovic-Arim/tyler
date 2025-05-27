package tyler.server.dto.task;

import jakarta.validation.constraints.*;
import tyler.server.validation.constraints.consistentdates.ConsistentTaskDates;

import java.time.LocalDate;

import static tyler.server.Constants.*;

@ConsistentTaskDates
public record TaskRequestDTO(
        Long parentId,

        @NotBlank(message = "Task name cannot be blank")
        @Size(min = 3, max = MAX_TASK_NAME_LENGTH, message = "Task name must be between 3 and " + MAX_TASK_NAME_LENGTH + " characters")
        String name,

        @Size(max = MAX_TASK_DESCRIPTION_LENGTH, message = "Description cannot exceed " + MAX_TASK_DESCRIPTION_LENGTH + " characters")
        String description,

        @FutureOrPresent(message = "Due date must be in the future or present")
        LocalDate dueDate,

        @NotNull(message = "Deadline is required")
        @FutureOrPresent(message = "Deadline must be in the future or present")
        LocalDate deadline,

        @NotNull(message = "Priority is required")
        Long priorityId
) {}