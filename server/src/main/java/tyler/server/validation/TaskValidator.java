package tyler.server.validation;

import jakarta.validation.ConstraintViolationException;
import org.springframework.stereotype.Component;
import tyler.server.entity.Task;
import tyler.server.exception.BusinessValidationException;

@Component
public class TaskValidator {
    public void validate(Task task) {
        if (task.getDueDate() != null && task.getDeadline() != null
                && task.getDueDate().isAfter(task.getDeadline())) {
            throw new ConstraintViolationException("Due date cannot be after deadline", null);
        }

        if (task.getParent() != null) {
            if (task.getDueDate() != null && task.getParent().getDueDate() != null &&
                    task.getDueDate().isAfter(task.getParent().getDueDate())) {
                throw new ConstraintViolationException(
                        "Subtask due date cannot be later than parent's due date", null
                );
            }

            if (task.getDeadline() != null && task.getParent().getDeadline() != null &&
                    task.getDeadline().isAfter(task.getParent().getDeadline())) {
                throw new ConstraintViolationException(
                        "Deadline cannot be after parent task's deadline", null
                );
            }

            int subtaskXpSum = task.getParent().getSubtasks().stream()
                    .mapToInt(subtask -> subtask.getPriority().getXp())
                    .sum();

            if (task.getPriority() != null && subtaskXpSum > task.getParent().getPriority().getXp()) {
                throw new BusinessValidationException("Subtask xp cannot exceed parent's xp");
            }
        }
    }
}
