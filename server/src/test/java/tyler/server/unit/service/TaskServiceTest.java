package tyler.server.unit.service;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tyler.server.entity.Priority;
import tyler.server.entity.Task;
import tyler.server.dto.task.TaskRequestDTO;
import tyler.server.dto.task.TaskResponseDTO;
import tyler.server.mapper.TaskMapper;
import tyler.server.exception.ResourceNotFoundException;
import tyler.server.repository.TaskRepository;
import tyler.server.service.ProgressService;
import tyler.server.service.TaskService;
import tyler.server.validation.TaskValidator;
import tyler.server.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskMapper taskMapper;
    @Mock private TaskValidator taskValidator;
    @Mock private ProgressService progressService;
    @InjectMocks private TaskService taskService;

    private LocalDate today, tomorrow;
    private Priority priority;
    private TaskRequestDTO defaultRequestDTO;
    private TaskResponseDTO responseDTO;
    private Task baseTask;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        tomorrow = today.plusDays(1);
        priority = Priority.builder().id(1L).name("HIGH").xp((byte) 10).build();

        defaultRequestDTO = new TaskRequestDTO(null, "Valid Task", "A well-formed description", today, tomorrow, 1L);
        responseDTO = new TaskResponseDTO(1L, 0, "Valid Task", "A well-formed description", today.toString(), (byte) 10, false);
        baseTask = Task.builder().id(1L).name("Valid Task").description("A well-formed description").dueDate(today).deadline(tomorrow).priority(priority).done(false).build();
    }

    private TaskRequestDTO requestWithParent(Long parentId) {
        return new TaskRequestDTO(parentId, "Subtask", "A subtask description", today, tomorrow, 1L);
    }

    private TaskRequestDTO requestWithPriority(Long priorityId) {
        return new TaskRequestDTO(null, "Task", "Description", today, tomorrow, priorityId);
    }

    @Test
    void getAllTasks_ShouldReturnListOfTaskDTOs() {
        when(taskRepository.findAllTasks()).thenReturn(List.of(responseDTO));

        var result = taskService.getAllTasks();

        assertThat(result).hasSize(1).first().satisfies(task -> assertThat(task.name()).isEqualTo("Valid Task"));
        verify(taskRepository).findAllTasks();
    }

    @Test
    void getTaskById_ShouldHandleFoundAndNotFoundCases() {
        when(taskRepository.findTaskById(1L)).thenReturn(Optional.of(responseDTO));
        assertThat(taskService.getTaskById(1L).name()).isEqualTo("Valid Task");

        when(taskRepository.findTaskById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> taskService.getTaskById(999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void saveTask_ShouldHandleValidSaveAndFailureCases() {
        var mappedTask = baseTask.toBuilder().id(null).build();

        when(taskMapper.RequestDtoToTask(defaultRequestDTO)).thenReturn(mappedTask);
        when(taskRepository.save(mappedTask)).thenReturn(baseTask);
        assertThat(taskService.saveTask(defaultRequestDTO)).isEqualTo(1L);

        when(taskRepository.save(mappedTask)).thenThrow(new ConstraintViolationException("DB fail", null));
        assertThatThrownBy(() -> taskService.saveTask(defaultRequestDTO)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void saveTask_ShouldHandleNullAndInvalidPriority() {
        var request = requestWithPriority(null);
        var mappedTask = baseTask.toBuilder().priority(null).build();
        when(taskMapper.RequestDtoToTask(request)).thenReturn(mappedTask);
        when(taskRepository.save(mappedTask)).thenReturn(baseTask);
        assertThat(taskService.saveTask(request)).isEqualTo(1L);

        var invalidRequest = requestWithPriority(999L);
        when(taskMapper.RequestDtoToTask(invalidRequest)).thenThrow(new ConstraintViolationException("Invalid priority", null));
        assertThatThrownBy(() -> taskService.saveTask(invalidRequest)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void saveTask_ShouldHandleParentAndSubtaskCases() {
        var parent = baseTask;
        var request = requestWithParent(1L);
        var subtask = baseTask.toBuilder().id(null).name("Subtask").parent(parent).build();
        var savedSubtask = subtask.toBuilder().id(2L).build();

        when(taskMapper.RequestDtoToTask(request)).thenReturn(subtask);
        when(taskRepository.save(subtask)).thenReturn(savedSubtask);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(parent));

        assertThat(taskService.saveTask(request)).isEqualTo(2L);
    }

    @Test
    void saveTask_ShouldThrow_WhenParentInvalid() {
        var request = requestWithParent(999L);
        when(taskMapper.RequestDtoToTask(request)).thenThrow(new ConstraintViolationException("Invalid parent", null));
        assertThatThrownBy(() -> taskService.saveTask(request)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void updateTask_ShouldUpdateIfExists_ElseThrow() {
        var updated = baseTask;
        when(taskRepository.existsById(1L)).thenReturn(true);
        when(taskMapper.RequestDtoToTask(defaultRequestDTO)).thenReturn(updated);

        taskService.updateTask(1L, defaultRequestDTO);
        verify(taskRepository).save(argThat(task -> task.getId() == 1L));

        when(taskRepository.existsById(999L)).thenReturn(false);
        assertThatThrownBy(() -> taskService.updateTask(999L, defaultRequestDTO)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateSubTask_ShouldPreserveParentInfo() {
        var parent = baseTask;
        var subtask = baseTask.toBuilder().id(2L).name("Updated Subtask").parent(parent).build();
        var request = requestWithParent(1L);

        when(taskRepository.existsById(1L)).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(taskMapper.RequestDtoToTask(request)).thenReturn(subtask);

        taskService.updateTask(1L, request);
        assertThat(subtask.getParent().getName()).isEqualTo(parent.getName());
        verify(taskRepository).save(subtask);
    }

    @Test
    void markTaskAsDone_ShouldHandleValidAndMissingCases() {
        var task = baseTask.toBuilder().done(false).build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        taskService.markTaskAsDone(1L);
        assertThat(task.isDone()).isTrue();

        when(taskRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> taskService.markTaskAsDone(999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markParentAsDone_ShouldMarkSubtasks() {
        var parent = baseTask.toBuilder().done(false).build();
        var sub = baseTask.toBuilder().id(2L).parent(parent).done(false).build();
        parent.getSubtasks().add(sub);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(parent));
        taskService.markTaskAsDone(1L);

        assertThat(parent.isDone()).isTrue();
        assertThat(sub.isDone()).isTrue();
    }

    @Test
    void deleteTask_ShouldDeleteIfExists_ElseThrow() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(baseTask));
        taskService.deleteTask(1L);
        verify(taskRepository).deleteById(1L);

        when(taskRepository.findById(999L)).thenThrow(new ResourceNotFoundException("Invalid task"));
        assertThatThrownBy(() -> taskService.deleteTask(999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteParentTask_ShouldClearSubtasks() {
        var parent = baseTask;
        var sub = baseTask.toBuilder().id(2L).parent(parent).build();
        parent.getSubtasks().add(sub);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(parent));

        taskService.deleteTask(1L);
        assertThat(parent.getSubtasks()).isEmpty();
    }

    @Test
    void deleteSubtask_ShouldRemoveFromParent() {
        var parent = baseTask;
        var sub = baseTask.toBuilder().id(2L).parent(parent).build();
        parent.getSubtasks().add(sub);

        when(taskRepository.findById(2L)).thenReturn(Optional.of(sub));
        taskService.deleteTask(2L);

        verify(taskRepository).deleteById(2L);
        assertThat(parent.getSubtasks()).doesNotContain(sub);
    }

    @Test
    void markTaskAsDone_ShouldHandleOffDayCorrectly() {
        User user = User.builder()
                .id(1L)
                .currentXp(0)
                .dailyXpQuota(100)
                .currentStreak(0)
                .build();

        Task task = baseTask.toBuilder()
                .user(user)
                .done(false)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskService.markTaskAsDone(1L);

        verify(progressService).handleTaskCompletion(task);
        assertThat(task.isDone()).isTrue();
    }
}
