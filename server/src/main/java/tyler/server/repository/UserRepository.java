package tyler.server.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tyler.server.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    @Query("SELECT u FROM User u JOIN FETCH u.daysOff d WHERE d = CURRENT_DATE")
    List<User> findUsersWithDayOffToday();
    @Query(
      "SELECT u " +
      "FROM User u " +
      "WHERE CURRENT_DATE NOT IN (SELECT d FROM u.daysOff d) " +
      "AND u.lastAchievedDate < CURRENT_DATE"
    )
    List<User> findUsersWhoAreNotOffAndMissedDailyQuotaToday();
}
