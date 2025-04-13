package tyler.server.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tyler.server.entity.Task;
import tyler.server.entity.dto.TaskDTO;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends CrudRepository<Task, Long> {
    @Query("SELECT new tyler.server.entity.dto.TaskDTO(t.id, t.name, t.description, " +
            "t.dueDate, t.deadline, t.xp, t.done) FROM Task t")
    List<TaskDTO> findAllTasks();

    @Query("SELECT new tyler.server.entity.dto.TaskDTO(t.id, t.name, t.description, " +
            "t.dueDate, t.deadline, t.xp, t.done) FROM Task t WHERE t.id = ?1")
    Optional<TaskDTO> findTaskById(Long id);
}
