package tyler.server.validation;

import jakarta.validation.ConstraintViolationException;
import org.springframework.stereotype.Component;
import tyler.server.entity.Task;
import tyler.server.exception.BusinessValidationException;

@Component
public class TaskValidator {
    public void validate(Task task) {
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
                    .mapToInt(Task::getRemainingXp)
                    .sum();

            if (task.getPriority() != null && subtaskXpSum > task.getParent().getRemainingXp()) {
                throw new BusinessValidationException("Subtask xp cannot exceed parent's xp");
            }
        }
        if (!task.getSubtasks().isEmpty()) {
            int subtasksXpSum = task.getSubtasks().stream()
                    .mapToInt(Task::getRemainingXp)
                    .sum();
            if (task.getPriority() != null && task.getPriority().getXp() < subtasksXpSum) {
                throw new BusinessValidationException("Task xp cannot be smaller than the xp of all the subtasks combined");
            }
        }
    }
}
