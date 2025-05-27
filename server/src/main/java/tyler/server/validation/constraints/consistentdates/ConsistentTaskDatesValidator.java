package tyler.server.validation.constraints.consistentdates;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import tyler.server.entity.Task;
import tyler.server.dto.task.TaskRequestDTO;

public class ConsistentTaskDatesValidator implements ConstraintValidator<ConsistentTaskDates, Object> {
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        return switch (value) {
            case Task task -> validateTask(task, context);
            case TaskRequestDTO dto -> validateTaskDTO(dto, context);
            case null, default -> true;
        };
    }

    private boolean validateTask(Task task, ConstraintValidatorContext context) {
        if (task.getDueDate() != null && task.getDeadline() != null &&
                task.getDueDate().isAfter(task.getDeadline())) {
            context.buildConstraintViolationWithTemplate(
                            "Due date cannot be after task's deadline")
                    .addPropertyNode("dueDate")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }

    private boolean validateTaskDTO(TaskRequestDTO dto, ConstraintValidatorContext context) {
        if (dto.dueDate() != null && dto.deadline() != null &&
                dto.dueDate().isAfter(dto.deadline())) {
            context.buildConstraintViolationWithTemplate(
                            "Due date cannot be after task's deadline")
                    .addPropertyNode("dueDate")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}