package tyler.server.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tyler.server.entity.Task;
import tyler.server.dto.task.TaskResponseDTO;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends CrudRepository<Task, Long> {
    @Query("SELECT new tyler.server.dto.task.TaskResponseDTO(t.id, SIZE(t.subtasks), t.name, t.description, " +
            "CAST(t.dueDate AS string), t.priority.xp, t.done) FROM Task t")
    List<TaskResponseDTO> findAllTasks();

    @Query("SELECT new tyler.server.dto.task.TaskResponseDTO(t.id, SIZE(t.subtasks), t.name, t.description, " +
            "CAST(t.dueDate AS string), t.priority.xp, t.done) FROM Task t WHERE t.id = ?1")
    Optional<TaskResponseDTO> findTaskById(Long id);
}