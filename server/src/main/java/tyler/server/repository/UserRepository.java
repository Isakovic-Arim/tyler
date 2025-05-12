package tyler.server.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tyler.server.entity.User;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {}
