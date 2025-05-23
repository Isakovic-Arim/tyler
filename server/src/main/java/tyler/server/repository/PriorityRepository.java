package tyler.server.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tyler.server.entity.Priority;

import java.util.List;

@Repository
public interface PriorityRepository extends CrudRepository<Priority, Long> {
    @Query("select p from Priority p")
    List<Priority> findAllPriorities();
}
