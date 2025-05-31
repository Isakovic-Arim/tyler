package tyler.server.integration.resource.user;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import tyler.server.integration.resource.BaseResourceTest;
import tyler.server.repository.PriorityRepository;
import tyler.server.repository.RefreshTokenRepository;
import tyler.server.repository.UserRepository;
import tyler.server.entity.User;
import tyler.server.entity.Task;
import tyler.server.entity.Priority;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserResourceTest extends BaseResourceTest {

    @Autowired
    private PriorityRepository priorityRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Priority priority;
    private User user;
    private Map<String, String> cookies;

    @BeforeAll
    void setup() {
        priority = Priority.builder()
                .name("High")
                .xp((byte) 5)
                .build();
        user = User.builder()
                .username("user")
                .passwordHash(passwordEncoder.encode("test"))
                .currentXp(0)
                .dailyXpQuota(10)
                .currentStreak(0)
                .daysOffPerWeek((byte) 2)
                .build();
        priorityRepository.save(priority);
        userRepository.save(user);

        cookies = getAuthCookies(user.getUsername(), "test");
    }

    @AfterEach
    void cleanUpEach() {
        user.setDaysOff(new HashSet<>());
        user.setDaysOffPerWeek((byte) 2);
        userRepository.save(user);
    }

    @AfterAll
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.delete(user);
    }

    @Test
    @WithMockUser(username = "user")
    void setDayOff_alreadyHasDayOff_returnsBadRequest() {
        LocalDate dayOff = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        user.getDaysOff().add(dayOff);
        userRepository.save(user);

        givenCookies(cookies)
                .body(dayOff)
                .when()
                .post("/users/me/day-off")
                .then()
                .statusCode(400);
    }

    @Test
    @WithMockUser(username = "user")
    void removeDayOff_validDayOff_returnsNoContent() {
        LocalDate dayOff = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        user.getDaysOff().add(dayOff);
        user.setDaysOffPerWeek((byte) 1);
        userRepository.save(user);

        givenCookies(cookies)
                .when()
                .queryParam("date", dayOff.toString())
                .delete("/users/me/day-off")
                .then()
                .statusCode(204);
    }

    @Test
    @WithMockUser(username = "user")
    void removeDayOff_today_returnsBadRequest() {
        LocalDate today = LocalDate.now();
        user.getDaysOff().add(today);
        userRepository.save(user);

        givenCookies(cookies)
                .body(today)
                .when()
                .delete("/users/me/day-off")
                .then()
                .statusCode(400);
    }

    @Test
    @WithMockUser(username = "user")
    void setDayOff_hasNoMoreDaysOff_returnsBadRequest() {
        user.setDaysOffPerWeek((byte) 0);
        userRepository.save(user);
        LocalDate dayOff = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        givenCookies(cookies)
                .body(dayOff)
                .when()
                .post("/users/me/day-off")
                .then()
                .statusCode(400);
    }

    @Test
    @WithMockUser(username = "user")
    void patchDayOff_validDateInCurrentWeek_returnsNoContent() {
        LocalDate validDayOff = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        givenCookies(cookies)
                .body(validDayOff)
                .when()
                .post("/users/me/day-off")
                .then()
                .statusCode(204);
    }

    @Test
    @WithMockUser(username = "user")
    void patchDayOff_invalidDateOutsideCurrentWeek_returnsBadRequest() {
        LocalDate invalidDayOff = LocalDate.now().plusWeeks(2);

        givenCookies(cookies)
                .body(invalidDayOff)
                .when()
                .post("/users/me/day-off")
                .then()
                .statusCode(400);
    }

    @Test
    @WithMockUser(username = "user")
    void setDayOff_ShouldRelocateTasks() {
        Task urgentTask = Task.builder()
                .name("Urgent Task")
                .dueDate(LocalDate.now().plusDays(1))
                .deadline(LocalDate.now().plusDays(2))
                .priority(priority)
                .user(user)
                .build();

        Task normalTask = Task.builder()
                .name("Normal Task")
                .dueDate(LocalDate.now().plusDays(1))
                .deadline(LocalDate.now().plusDays(3))
                .priority(priority)
                .user(user)
                .build();

        user.addTask(urgentTask);
        user.addTask(normalTask);
        userRepository.save(user);

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        givenCookies(cookies)
                .body(tomorrow)
                .when()
                .post("/users/me/day-off")
                .then()
                .statusCode(204);

        // Refresh user from database
        user = entityManagerFactory.createEntityManager().createQuery("SELECT u FROM User u JOIN u.tasks WHERE u.id = :id", User.class)
                .setParameter("id", user.getId())
                .getSingleResult();

        // Verify tasks were relocated correctly
        Task relocatedUrgentTask = user.getTasks().stream()
                .filter(t -> t.getName().equals("Urgent Task"))
                .findFirst()
                .orElseThrow();
        Task relocatedNormalTask = user.getTasks().stream()
                .filter(t -> t.getName().equals("Normal Task"))
                .findFirst()
                .orElseThrow();

        // Urgent task should stay on deadline day
        assertThat(relocatedUrgentTask.getDueDate()).isEqualTo(LocalDate.now().plusDays(2));
        // Normal task should be moved to next available day
        assertThat(relocatedNormalTask.getDueDate()).isEqualTo(tomorrow.plusDays(1));
    }

    @Test
    @WithMockUser(username = "user")
    void removeDayOff_ShouldRelocateTasks() {
        Task task = Task.builder()
                .name("Test Task")
                .dueDate(LocalDate.now().plusDays(1))
                .deadline(LocalDate.now().plusDays(2))
                .priority(priority)
                .user(user)
                .build();

        user.addTask(task);
        userRepository.save(user);

        LocalDate sunday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        user.getDaysOff().add(sunday);
        user.setDaysOffPerWeek((byte) 1);
        userRepository.save(user);

        givenCookies(cookies)
                .when()
                .queryParam("date", sunday.toString())
                .delete("/users/me/day-off")
                .then()
                .statusCode(204);

        user = entityManagerFactory.createEntityManager()
                .createQuery("SELECT u FROM User u JOIN u.tasks WHERE u.id = :id", User.class)
                .setParameter("id", user.getId())
                .getSingleResult();

        Task relocatedTask = user.getTasks().stream()
                .filter(t -> t.getName().equals("Test Task"))
                .findFirst()
                .orElseThrow();

        assertThat(relocatedTask.getDueDate()).isEqualTo(sunday);
    }

    @Test
    @WithMockUser(username = "user")
    void setDayOff_ShouldHandleSubtasks() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // Create parent task and subtask
        Task parentTask = Task.builder()
                .name("Parent Task")
                .dueDate(tomorrow)
                .deadline(tomorrow.plusDays(1))
                .priority(priority)
                .user(user)
                .build();

        Task subtask = Task.builder()
                .name("Subtask")
                .dueDate(tomorrow)
                .deadline(tomorrow.plusDays(1))
                .priority(priority)
                .user(user)
                .build();

        parentTask.addSubtask(subtask);
        user.addTask(parentTask);
        userRepository.save(user);

        givenCookies(cookies)
                .body(tomorrow)
                .when()
                .post("/users/me/day-off")
                .then()
                .statusCode(204);

        // Refresh user from database
        user = entityManagerFactory.createEntityManager().createQuery("SELECT u FROM User u JOIN u.tasks WHERE u.id = :id", User.class)
                .setParameter("id", user.getId())
                .getSingleResult();

        // Verify both parent and subtask were relocated
        Task relocatedParentTask = user.getTasks().stream()
                .filter(t -> t.getName().equals("Parent Task"))
                .findFirst()
                .orElseThrow();
        Task relocatedSubtask = relocatedParentTask.getSubtasks().stream()
                .filter(t -> t.getName().equals("Subtask"))
                .findFirst()
                .orElseThrow();

        assertThat(relocatedParentTask.getDueDate()).isEqualTo(tomorrow.plusDays(1));
        assertThat(relocatedSubtask.getDueDate()).isEqualTo(tomorrow.plusDays(1));
    }
}
