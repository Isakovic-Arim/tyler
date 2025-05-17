package tyler.server.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tyler.server.dto.task.TaskRequestDTO;
import tyler.server.dto.task.TaskResponseDTO;
import tyler.server.entity.User;
import tyler.server.service.TaskService;

import java.lang.annotation.Target;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URI;
import java.util.List;

@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "@userService.getUserFromJwt(#this)")
@interface CurrentUser {}

@RestController
@RequestMapping("/tasks")
@Validated
public class TaskResource {
    private final TaskService taskService;

    public TaskResource(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> getTasks() {
        return ResponseEntity.ok().body(taskService.getAllTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @PostMapping
    public ResponseEntity<Void> createTask(@CurrentUser User user, @Valid @RequestBody TaskRequestDTO task) {
        Long id = taskService.saveTask(user, task);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequestDTO task) {
        taskService.updateTask(id, task);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/done")
    public ResponseEntity<Void> markTaskAsDone(@PathVariable Long id) {
        taskService.markTaskAsDone(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
