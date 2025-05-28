package tyler.server.unit.service;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.test.context.support.WithMockUser;
import tyler.server.entity.Priority;
import tyler.server.entity.Task;
import tyler.server.dto.task.TaskRequestDTO;
import tyler.server.dto.task.TaskResponseDTO;
import tyler.server.mapper.TaskMapper;
import tyler.server.exception.ResourceNotFoundException;
import tyler.server.repository.PriorityRepository;
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

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private PriorityRepository priorityRepository;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private TaskValidator taskValidator;
    @Mock
    private ProgressService progressService;
    @Mock
    private JdbcMutableAclService aclService;
    @InjectMocks
    private TaskService taskService;

    private LocalDate today, tomorrow;
    private Priority priority;
    private TaskRequestDTO defaultRequestDTO;
    private TaskResponseDTO responseDTO;
    private Task baseTask;
    private User testUser;
    private MutableAcl mockAcl;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        tomorrow = today.plusDays(1);
        priority = Priority.builder().id(1L).name("HIGH").xp((byte) 10).build();
        testUser = User.builder().id(1L).username("testuser").currentXp(0).dailyXpQuota(100).currentStreak(0).build();
        mockAcl = mock(MutableAcl.class);

        defaultRequestDTO = new TaskRequestDTO(null, "Valid Task", "A well-formed description", today, tomorrow, 1L);
        responseDTO = new TaskResponseDTO(1L, null, 0, "Valid Task", "A well-formed description", today.toString(), tomorrow.toString(), (byte) 10, false);
        baseTask = Task.builder().id(1L).name("Valid Task").description("A well-formed description")
                .dueDate(today).deadline(tomorrow).priority(priority).done(false).user(testUser).build();
    }

    private TaskRequestDTO requestWithParent(Long parentId) {
        return new TaskRequestDTO(parentId, "Subtask", "A subtask description", today, tomorrow, 1L);
    }

    private TaskRequestDTO requestWithPriority(Long priorityId) {
        return new TaskRequestDTO(null, "Task", "Description", today, tomorrow, priorityId);
    }

    @Test
    @WithMockUser(username = "testuser")
    void getAllTasks_ShouldReturnListOfTaskDTOs() {
        when(taskRepository.findAllTasks()).thenReturn(List.of(baseTask));
        when(taskMapper.toResponseDto(any(Task.class))).thenReturn(responseDTO);

        var result = taskService.getAllTasks();

        assertThat(result).hasSize(1).first().satisfies(task -> assertThat(task.name()).isEqualTo("Valid Task"));
        verify(taskRepository).findAllTasks();
    }

    @Test
    @WithMockUser(username = "testuser")
    void getTaskById_ShouldHandleFoundAndNotFoundCases() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(baseTask));
        when(taskMapper.toResponseDto(any(Task.class))).thenReturn(responseDTO);

        assertThat(taskService.getTaskById(1L).name()).isEqualTo("Valid Task");

        when(taskRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> taskService.getTaskById(999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @WithMockUser(username = "testuser")
    void saveTask_ShouldCreateAclAndHandleValidSave() {
        var mappedTask = baseTask.toBuilder().id(null).build();
        Sid userSid = new PrincipalSid(testUser.getUsername());

        when(taskMapper.toTask(defaultRequestDTO)).thenReturn(mappedTask);
        when(priorityRepository.findById(1L)).thenReturn(Optional.of(priority));
        when(taskRepository.save(mappedTask)).thenReturn(baseTask);
        when(aclService.createAcl(any(ObjectIdentity.class))).thenReturn(mockAcl);

        assertThat(taskService.saveTask(testUser, defaultRequestDTO)).isEqualTo(1L);

        verify(aclService).createAcl(any(ObjectIdentity.class));
        verify(mockAcl).insertAce(eq(0), eq(BasePermission.READ), eq(userSid), eq(true));
        verify(mockAcl).insertAce(eq(1), eq(BasePermission.WRITE), eq(userSid), eq(true));
        verify(mockAcl).insertAce(eq(2), eq(BasePermission.DELETE), eq(userSid), eq(true));
        verify(aclService).updateAcl(mockAcl);
    }

    @Test
    @WithMockUser(username = "testuser")
    void saveTask_ShouldHandleNullAndInvalidPriority() {
        var request = requestWithPriority(null);
        var mappedTask = baseTask.toBuilder().priority(null).build();
        when(taskMapper.toTask(request)).thenReturn(mappedTask);

        assertThatThrownBy(() -> taskService.saveTask(testUser, request)).isInstanceOf(ConstraintViolationException.class);

        var invalidRequest = requestWithPriority(999L);
        when(taskMapper.toTask(invalidRequest)).thenThrow(new ConstraintViolationException("Invalid priority", null));
        assertThatThrownBy(() -> taskService.saveTask(testUser, invalidRequest)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @WithMockUser(username = "testuser")
    void saveTask_ShouldHandleParentAndSubtaskCases() {
        var parent = baseTask;
        var request = requestWithParent(1L);
        var subtask = baseTask.toBuilder().id(null).name("Subtask").parent(parent).build();
        var savedSubtask = subtask.toBuilder().id(2L).build();

        when(taskMapper.toTask(request)).thenReturn(subtask);
        when(priorityRepository.findById(1L)).thenReturn(Optional.of(priority));
        when(taskRepository.save(subtask)).thenReturn(savedSubtask);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(aclService.createAcl(any(ObjectIdentity.class))).thenReturn(mockAcl);

        assertThat(taskService.saveTask(testUser, request)).isEqualTo(2L);
    }

    @WithMockUser(username = "testuser")
    @Test
    void saveTask_subtaskExceedsParentRemainingXp_shouldThrow() {
        Task parent = Task.builder()
                .id(1L)
                .priority(Priority.builder().id(1L).xp((byte)5).build())
                .remainingXp((byte)3)
                .done(false)
                .user(testUser)
                .build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(parent));

        TaskRequestDTO request = new TaskRequestDTO(
                1L, "Child Task", "desc", null, null, 2L
        );
        Task subtask = Task.builder()
                .priority(Priority.builder().id(2L).xp((byte)4).build())
                .done(false)
                .user(testUser)
                .build();
        when(taskMapper.toTask(request)).thenReturn(subtask);
        when(priorityRepository.findById(2L)).thenReturn(Optional.of(subtask.getPriority()));

        doThrow(new ConstraintViolationException("cannot exceed parent", null))
                .when(taskValidator).validate(subtask);

        assertThatThrownBy(() -> taskService.saveTask(testUser, request))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("cannot exceed parent");
    }

    @Test
    void saveTask_ShouldFailIfDueDateAfterDeadline() {
        var invalidRequest = new TaskRequestDTO(null, "Invalid", "desc",
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(3), 1L);
        var baseTask = Task.builder()
                .id(1L)
                .name("Invalid")
                .description("desc")
                .dueDate(LocalDate.now().plusDays(5))
                .deadline(LocalDate.now().plusDays(3))
                .priority(priority)
                .done(false)
                .user(testUser)
                .build();

        when(taskMapper.toTask(invalidRequest))
                .thenReturn(baseTask.toBuilder()
                        .dueDate(LocalDate.now().plusDays(5))
                        .deadline(LocalDate.now().plusDays(3))
                        .build());
        when(priorityRepository.findById(1L)).thenReturn(Optional.of(priority));
        doThrow(new ConstraintViolationException("Due date cannot be after deadline", null))
                .when(taskValidator).validate(baseTask);

        assertThatThrownBy(() -> taskService.saveTask(testUser, invalidRequest))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Due date cannot be after deadline");
    }

    @Test
    void saveTask_ShouldFailIfSubtaskXpExceedsParent() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(baseTask));
        var childReq = new TaskRequestDTO(1L, "Subtask", "desc",
                LocalDate.now(), LocalDate.now().plusDays(1), 1L);
        when(taskMapper.toTask(childReq))
                .thenReturn(baseTask.toBuilder()
                        .priority(Priority.builder().xp((byte) 15).build())
                        .build());

        var parent = baseTask;
        var child = baseTask.toBuilder().id(2L).priority(
                Priority.builder().xp((byte) 12).build()).parent(parent).build();

        when(taskMapper.toTask(childReq)).thenReturn(child);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(priorityRepository.findById(1L)).thenReturn(Optional.of(priority));
        doThrow(new ConstraintViolationException("Subtasks' XP cannot exceed parent", null))
                .when(taskValidator).validate(child);

        assertThatThrownBy(() -> taskService.saveTask(testUser, childReq))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Subtasks' XP cannot exceed parent");
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateTask_ShouldUpdateIfExists_ElseThrow() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(baseTask));
        when(priorityRepository.findById(1L)).thenReturn(Optional.of(priority));

        taskService.updateTask(1L, defaultRequestDTO);

        when(taskRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> taskService.updateTask(999L, defaultRequestDTO)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateTask_ShouldThrowIfParentNotFound() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(baseTask));
        when(priorityRepository.findById(1L)).thenReturn(Optional.of(priority));

        var request = new TaskRequestDTO(999L, "Updated Name", "Desc", today, tomorrow, 1L);

        doThrow(new ConstraintViolationException("Parent Task with ID 999 does not exist", null)).when(taskRepository).findById(999L);

        assertThatThrownBy(() -> taskService.updateTask(1L, request)).isInstanceOf(ConstraintViolationException.class);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateTask_ShouldPreserveSubtasks() {
        var parent = baseTask.toBuilder().build();
        var subtask = baseTask.toBuilder().id(2L).parent(parent).build();
        parent.getSubtasks().add(subtask);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(priorityRepository.findById(1L)).thenReturn(Optional.of(priority));

        taskService.updateTask(1L, defaultRequestDTO);
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateTask_ShouldThrowIfPriorityNotFound() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(baseTask));
        when(priorityRepository.findById(999L)).thenThrow(new ConstraintViolationException("Priority with ID 999 does not exist", null));

        var request = new TaskRequestDTO(null, "Updated Name", "Desc", today, tomorrow, 999L);

        assertThatThrownBy(() -> taskService.updateTask(1L, request)).isInstanceOf(ConstraintViolationException.class);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateTask_ShouldCallValidatorAndSave() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(baseTask));
        when(priorityRepository.findById(1L)).thenReturn(Optional.of(priority));

        taskService.updateTask(1L, defaultRequestDTO);
    }

    @Test
    @WithMockUser(username = "testuser")
    void markTaskAsDone_ShouldHandleValidAndMissingCases() {
        var task = baseTask.toBuilder().done(false).build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskService.markTaskAsDone(1L);
        assertThat(task.isDone()).isTrue();
        verify(progressService).handleTaskCompletion(task);

        when(taskRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> taskService.markTaskAsDone(999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @WithMockUser(username = "testuser")
    void markParentAsDone_ShouldMarkSubtasks() {
        var parent = baseTask.toBuilder().done(false).build();
        var sub = baseTask.toBuilder().id(2L).parent(parent).done(false).build();
        parent.getSubtasks().add(sub);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(parent));

        taskService.markTaskAsDone(1L);

        assertThat(parent.isDone()).isTrue();
        assertThat(sub.isDone()).isTrue();
        verify(progressService, times(2)).handleTaskCompletion(any(Task.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    void deleteTask_ShouldDeleteIfExists_ElseThrow() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(baseTask));
        taskService.deleteTask(1L);
        verify(taskRepository).deleteById(1L);

        when(taskRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> taskService.deleteTask(999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @WithMockUser(username = "testuser")
    void deleteParentTask_ShouldClearSubtasks() {
        var parent = baseTask;
        var sub = baseTask.toBuilder().id(2L).parent(parent).build();
        parent.getSubtasks().add(sub);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(parent));

        taskService.deleteTask(1L);
        assertThat(parent.getSubtasks()).isEmpty();
    }

    @Test
    @WithMockUser(username = "testuser")
    void deleteSubtask_ShouldRemoveFromParent() {
        var parent = baseTask;
        var sub = baseTask.toBuilder().id(2L).parent(parent).build();
        parent.getSubtasks().add(sub);

        when(taskRepository.findById(2L)).thenReturn(Optional.of(sub));

        taskService.deleteTask(2L);
        verify(taskRepository).deleteById(2L);
        assertThat(parent.getSubtasks()).doesNotContain(sub);
    }
}
