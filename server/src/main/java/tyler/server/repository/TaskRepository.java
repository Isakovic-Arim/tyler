package tyler.server.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Repository;
import tyler.server.entity.Task;

import java.util.List;

@Repository
public interface TaskRepository extends CrudRepository<Task, Long> {
    @PostFilter("hasPermission(filterObject, 'read')")
    @Query("SELECT t FROM Task t JOIN FETCH t.user")
    List<Task> findAllTasks();
    @Query("SELECT t FROM Task t JOIN FETCH t.user WHERE t.deadline < CURRENT_DATE")
    List<Task> findAllTasksOverDeadline();
}