package tyler.server.unit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tyler.server.entity.Task;
import tyler.server.entity.dto.TaskDTO;
import tyler.server.entity.mapper.TaskMapper;
import tyler.server.exception.ResourceNotFoundException;
import tyler.server.repository.TaskRepository;
import tyler.server.service.TaskService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private final LocalDate today = LocalDate.now();
    private final LocalDate tomorrow = today.plusDays(1);
    private final TaskDTO taskDTO = new TaskDTO(
            1L,
            "Valid Task",
            "A well-formed description",
            today,
            tomorrow,
            (byte) 10,
            false
    );

    @Test
    void getAllTasks_ShouldReturnListOfTaskDTOs() {
        when(taskRepository.findAllTasks()).thenReturn(List.of(taskDTO));

        List<TaskDTO> result = taskService.getAllTasks();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Valid Task");
        verify(taskRepository).findAllTasks();
    }

    @Test
    void getTaskById_ExistingId_ShouldReturnTaskDTO() {
        when(taskRepository.findTaskById(1L)).thenReturn(Optional.of(taskDTO));

        TaskDTO result = taskService.getTaskById(1L);

        assertThat(result.name()).isEqualTo("Valid Task");
        verify(taskRepository).findTaskById(1L);
    }

    @Test
    void getTaskById_NonExistingId_ShouldThrowException() {
        doThrow(new ResourceNotFoundException("Task with ID 999 does not exist"))
                .when(taskRepository).findTaskById(999L);

        assertThatThrownBy(() -> taskService.getTaskById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not exist");
        verify(taskRepository).findTaskById(999L);
    }

    @Test
    void saveTask_ShouldReturnGeneratedId() {
        Task mappedTask = TaskMapper.INSTANCE.DtoToTask(taskDTO);
        mappedTask.setId(1L);

        when(taskRepository.save(any(Task.class))).thenReturn(mappedTask);

        Long result = taskService.saveTask(taskDTO);

        assertThat(result).isEqualTo(1L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void saveTask_ShouldThrowException_WhenSaveFails() {
        doThrow(new IllegalArgumentException("DB fail")).when(taskRepository).save(any(Task.class));

        assertThatThrownBy(() -> taskService.saveTask(taskDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Error saving task");

        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void updateTask_ShouldSaveUpdatedTask_WhenIdExists() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.updateTask(1L, taskDTO);

        verify(taskRepository).existsById(1L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void updateTask_ShouldThrowException_WhenIdDoesNotExist() {
        when(taskRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.updateTask(999L, taskDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not exist");

        verify(taskRepository).existsById(999L);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void markTaskAsDone_ShouldSetDoneTrue_WhenTaskExists() {
        Task incompleteTask = new Task(
                1L, "Task", "desc", today, tomorrow, (byte) 10, false
        );
        when(taskRepository.findById(1L)).thenReturn(Optional.of(incompleteTask));

        taskService.markTaskAsDone(1L);

        assertThat(incompleteTask.isDone()).isTrue();
        verify(taskRepository).findById(1L);
    }

    @Test
    void markTaskAsDone_ShouldThrowException_WhenTaskNotFound() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.markTaskAsDone(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not exist");

        verify(taskRepository).findById(999L);
    }

    @Test
    void deleteTask_ShouldDelete_WhenIdExists() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.deleteTask(1L);

        verify(taskRepository).existsById(1L);
        verify(taskRepository).deleteById(1L);
    }

    @Test
    void deleteTask_ShouldThrowException_WhenIdDoesNotExist() {
        when(taskRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not exist");

        verify(taskRepository).existsById(999L);
        verify(taskRepository, never()).deleteById(any());
    }
}