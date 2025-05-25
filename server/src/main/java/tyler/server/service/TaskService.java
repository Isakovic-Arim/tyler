package tyler.server.service;

import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import tyler.server.dto.task.TaskRequestDTO;
import tyler.server.dto.task.TaskResponseDTO;
import tyler.server.entity.User;
import tyler.server.mapper.TaskMapper;
import tyler.server.exception.ResourceNotFoundException;
import tyler.server.repository.PriorityRepository;
import tyler.server.repository.TaskRepository;
import tyler.server.entity.Task;
import tyler.server.validation.TaskValidator;

import java.util.List;

@Service
@Validated
public class TaskService {
    private final TaskRepository taskRepository;
    private final PriorityRepository priorityRepository;
    private final TaskMapper taskMapper;
    private final TaskValidator validator;
    private final ProgressService progressService;
    private final JdbcMutableAclService aclService;

    public TaskService(
            TaskRepository taskRepository, PriorityRepository priorityRepository,
            TaskMapper taskMapper,
            TaskValidator validator,
            ProgressService progressService,
            JdbcMutableAclService aclService) {
        this.taskRepository = taskRepository;
        this.priorityRepository = priorityRepository;
        this.taskMapper = taskMapper;
        this.validator = validator;
        this.progressService = progressService;
        this.aclService = aclService;
    }

    public List<TaskResponseDTO> getAllTasks() {
        return taskRepository.findAllTasks().stream()
                .map(taskMapper::toResponseDto).toList();
    }

    @PostAuthorize("hasPermission(#id, 'tyler.server.entity.Task', 'read')")
    public TaskResponseDTO getTaskById(Long id) {
        Task task = findTaskById(id);
        return taskMapper.toResponseDto(task);
    }

    @Transactional
    public Long saveTask(User user, @Valid TaskRequestDTO request) {
        Task task = taskMapper.toTask(request);

        setTaskPriority(task, request.priorityId());
        user.addTask(task);

        if (request.parentId() != null) {
            linkToParent(task, request.parentId());
        }

        validator.validate(task);
        task = taskRepository.save(task);

        createAclForTask(task, user);
        return task.getId();
    }

    @PostAuthorize("hasPermission(#id, 'tyler.server.entity.Task', 'write')")
    @Transactional
    public void updateTask(Long id, @Valid TaskRequestDTO request) {
        Task existing = findTaskById(id);

        existing.setName(request.name());
        existing.setDescription(request.description());
        existing.setDueDate(request.dueDate());
        existing.setDeadline(request.deadline());
        setTaskPriority(existing, request.priorityId());

        if (request.parentId() != null) {
            linkToParent(existing, request.parentId());
        }

        validator.validate(existing);
    }

    @PostAuthorize("hasPermission(#id, 'tyler.server.entity.Task', 'write')")
    @Transactional
    public void markTaskAsDone(Long id) {
        Task task = findTaskById(id);

        task.setDone(true);
        progressService.handleTaskCompletion(task);

        task.getSubtasks().forEach(subtask -> {
            subtask.setDone(true);
            progressService.handleTaskCompletion(subtask);
        });
    }

    @PostAuthorize("hasPermission(#id, 'tyler.server.entity.Task', 'delete')")
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

    private void setTaskPriority(Task task, Long priorityId) {
        task.setPriority(
                priorityRepository.findById(priorityId)
                        .orElseThrow(() -> new ConstraintViolationException(
                                "Priority with ID " + priorityId + " does not exist", null))
        );
    }

    private void createAclForTask(Task task, User user) {
        ObjectIdentity oid = new ObjectIdentityImpl(Task.class, task.getId());
        MutableAcl acl = aclService.createAcl(oid);

        Sid userSid = new PrincipalSid(user.getUsername());
        acl.insertAce(0, BasePermission.READ, userSid, true);
        acl.insertAce(1, BasePermission.WRITE, userSid, true);
        acl.insertAce(2, BasePermission.DELETE, userSid, true);

        aclService.updateAcl(acl);
    }
}
