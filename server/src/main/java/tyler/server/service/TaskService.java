package tyler.server.service;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import tyler.server.entity.dto.TaskDTO;
import tyler.server.entity.mapper.TaskMapper;
import tyler.server.exception.ResourceNotFoundException;
import tyler.server.repository.TaskRepository;
import tyler.server.entity.Task;

import java.util.List;

@Service
@Validated
public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAllTasks();
    }

    public TaskDTO getTaskById(Long id) {
        return taskRepository.findTaskById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with ID " + id + " does not exist"));
    }

    public Long saveTask(@Valid TaskDTO taskDTO) {
        Task task = TaskMapper.INSTANCE.DtoToTask(taskDTO);
        try {
            taskRepository.save(task);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Error saving task: " + e.getMessage());
        }
        return task.getId();
    }

    @Transactional
    public void updateTask(Long id, @Valid TaskDTO taskDTO) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task with ID " + id + " does not exist");
        }
        Task task = TaskMapper.INSTANCE.DtoToTask(taskDTO);
        task.setId(id);
        taskRepository.save(task);
    }

    @Transactional
    public void markTaskAsDone(Long id) {
        taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with ID " + id + " does not exist"))
                .setDone(true);
    }

    @Transactional
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task with ID " + id + " does not exist");
        }
        taskRepository.deleteById(id);
    }
}
