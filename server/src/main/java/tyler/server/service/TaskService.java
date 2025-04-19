package tyler.server.service;

import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import tyler.server.dto.task.TaskRequestDTO;
import tyler.server.dto.task.TaskResponseDTO;
import tyler.server.mapper.TaskMapper;
import tyler.server.exception.ResourceNotFoundException;
import tyler.server.repository.TaskRepository;
import tyler.server.entity.Task;
import tyler.server.validation.TaskValidator;

import java.util.List;

@Service
@Validated
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final TaskValidator validator;

    public TaskService(TaskRepository taskRepository, TaskMapper taskMapper, TaskValidator validator) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
        this.validator = validator;
    }

    public List<TaskResponseDTO> getAllTasks() {
        return taskRepository.findAllTasks();
    }

    public TaskResponseDTO getTaskById(Long id) {
        return taskRepository.findTaskById(id)
                .orElseThrow(() -> taskNotFound(id));
    }

    @Transactional
    public Long saveTask(@Valid TaskRequestDTO request) {
        Task task = taskMapper.RequestDtoToTask(request);

        if (request.parentId() != null) {
            linkToParent(task, request.parentId());
        }

        validator.validate(task);
        task = taskRepository.save(task);
        return task.getId();
    }

    @Transactional
    public void updateTask(Long id, @Valid TaskRequestDTO request) {
        if (!taskRepository.existsById(id)) {
            throw taskNotFound(id);
        }

        Task task = taskMapper.RequestDtoToTask(request);
        task.setId(id);

        if (request.parentId() != null) {
            linkToParent(task, request.parentId());
        }

        validator.validate(task);
        taskRepository.save(task);
    }

    @Transactional
    public void markTaskAsDone(Long id) {
        Task task = findTaskById(id);
        task.setDone(true);
        task.getSubtasks().forEach(subtask -> subtask.setDone(true));
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = findTaskById(id);
        task.getSubtasks().clear();

        if (task.getParent() != null) {
            task.getParent().removeSubtask(task);
        }

        taskRepository.deleteById(id);
    }

    private Task findTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> taskNotFound(id));
    }

    private ResourceNotFoundException taskNotFound(Long id) {
        return new ResourceNotFoundException("Task with ID " + id + " does not exist");
    }

    private void linkToParent(Task task, Long parentId) {
        Task parent = taskRepository.findById(parentId)
                .orElseThrow(() -> new ConstraintViolationException(
                        "Parent Task with ID " + parentId + " does not exist", null));
        parent.addSubtask(task);
    }
}