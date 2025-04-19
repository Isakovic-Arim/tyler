package tyler.server.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tyler.server.entity.Priority;

@Repository
public interface PriorityRepository extends CrudRepository<Priority, Long> { }
