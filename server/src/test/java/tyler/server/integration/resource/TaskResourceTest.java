package tyler.server.integration.resource;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tyler.server.entity.Priority;
import tyler.server.entity.Task;
import tyler.server.entity.User;
import tyler.server.dto.task.TaskRequestDTO;
import tyler.server.repository.PriorityRepository;
import tyler.server.repository.TaskRepository;
import tyler.server.repository.UserRepository;

import java.time.*;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.*;
import static tyler.server.Constants.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TaskResourceTest {
    private static final String TASKS_ENDPOINT = "/tasks";
    private static final String DEFAULT_DESCRIPTION = "Test description";
    private static final byte DEFAULT_XP = 3;
    private static final Priority DEFAULT_PRIORITY = Priority.builder()
            .name("HIGH")
            .xp(DEFAULT_XP)
            .build();

    @Container
    @ServiceConnection
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16.8-alpine3.20");

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private PriorityRepository priorityRepository;
    @Autowired
    private UserRepository userRepository;

    @BeforeAll
    void setUp(@Value("${local.server.port}") int port) {
        RestAssured.port = port;
        RestAssured.registerParser("text/plain", Parser.TEXT);
        priorityRepository.deleteAll();
        priorityRepository.save(DEFAULT_PRIORITY);
    }

    // region HELPERS
    private RequestSpecification givenJson() {
        return given().contentType(ContentType.JSON);
    }

    private Task createTestTask(String name, LocalDate deadline) {
        return Task.builder()
                .name(name)
                .description(DEFAULT_DESCRIPTION)
                .dueDate(null)
                .deadline(deadline)
                .done(false)
                .priority(DEFAULT_PRIORITY)
                .parent(null)
                .build();
    }

    private Task saveTask(String name, int daysFromNow) {
        return taskRepository.save(createTestTask(name, LocalDate.now().plusDays(daysFromNow)));
    }

    private TaskRequestDTO buildTaskRequest(Long parentId, String name, String description, LocalDate dueDate, LocalDate deadline, Long priorityId) {
        return new TaskRequestDTO(parentId, name, description, dueDate, deadline, priorityId);
    }

    private String postTaskAndReturnLocation(TaskRequestDTO task) {
        return givenJson()
                .body(task)
                .when()
                .post(TASKS_ENDPOINT)
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
    }

    private Priority createPriorityWithXp(String name, byte xp) {
        return priorityRepository.save(Priority.builder().name(name).xp(xp).build());
    }

    private void verifyTask(Long id, String name, String description, Byte xp, Boolean done) {
        var request = given().when().get(TASKS_ENDPOINT + "/{id}", id).then().statusCode(200);
        if (name != null) request.body("name", equalTo(name));
        if (description != null) request.body("description", equalTo(description));
        if (xp != null) request.body("xp", equalTo(xp.intValue()));
        if (done != null) request.body("done", equalTo(done));
    }

    private User createTestUser(int dailyXpQuota) {
        User user = User.builder()
                .currentXp(0)
                .dailyXpQuota(dailyXpQuota)
                .currentStreak(0)
                .lastAchievedDate(null)
                .build();
        userRepository.save(user);
        return user;
    }

    private Task createTaskWithUser(String name, User user, Priority priority) {
        Task task = Task.builder()
                .name(name)
                .description(DEFAULT_DESCRIPTION)
                .dueDate(null)
                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .build();
        user.addTask(task);
        taskRepository.save(task);
        return task;
    }
    // endregion

    // region POST
    @Test
    void postTask_validTask_returnsCreated() {
        TaskRequestDTO task = buildTaskRequest(null, "Valid Task", DEFAULT_DESCRIPTION, null, LocalDate.now().plusDays(1), 1L);
        postTaskAndReturnLocation(task);
    }

    @Test
    void postTask_emptyName_returnsBadRequest() {
        TaskRequestDTO task = buildTaskRequest(null, "", DEFAULT_DESCRIPTION, null, LocalDate.now().plusDays(1), 1L);
        givenJson().body(task).when().post(TASKS_ENDPOINT).then().statusCode(400);
    }

    @Test
    void postTask_tooLongName_returnsBadRequest() {
        String tooLongName = "a".repeat(MAX_TASK_NAME_LENGTH + 1);
        TaskRequestDTO task = buildTaskRequest(null, tooLongName, DEFAULT_DESCRIPTION, null, LocalDate.now().plusDays(1), 1L);
        givenJson().body(task).when().post(TASKS_ENDPOINT).then().statusCode(400);
    }

    @Test
    void postTask_tooLongDescription_returnsBadRequest() {
        TaskRequestDTO task = buildTaskRequest(null, "Valid Name", "a".repeat(MAX_TASK_DESCRIPTION_LENGTH + 1), null, LocalDate.now().plusDays(1), 1L);
        givenJson().body(task).when().post(TASKS_ENDPOINT).then().statusCode(400);
    }

    @Test
    void postTask_pastDeadline_returnsBadRequest() {
        TaskRequestDTO task = buildTaskRequest(null, "Valid Name", DEFAULT_DESCRIPTION, null, LocalDate.now().minusDays(1), 1L);
        givenJson().body(task).when().post(TASKS_ENDPOINT).then().statusCode(400);
    }

    @Test
    void postTask_nullDeadline_returnsBadRequest() {
        TaskRequestDTO task = buildTaskRequest(null, "Valid Name", DEFAULT_DESCRIPTION, null, null, 1L);
        givenJson().body(task).when().post(TASKS_ENDPOINT).then().statusCode(400);
    }

    @Test
    void postTask_pastDueDate_returnsBadRequest() {
        Map<String, Object> task = Map.of(
                "name", "Valid Name",
                "description", DEFAULT_DESCRIPTION,
                "dueDate", LocalDate.now().minusDays(1).toString(),
                "deadline", LocalDate.now().plusDays(1).toString(),
                "xp", DEFAULT_XP,
                "done", false
        );
        givenJson().body(task).when().post(TASKS_ENDPOINT).then().statusCode(400);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, MAX_PRIORITY_XP + 1})
    void postTask_invalidXp_returnsBadRequest(int invalidXp) {
        Map<String, Object> task = Map.of(
                "name", "Valid Name",
                "description", DEFAULT_DESCRIPTION,
                "deadline", LocalDate.now().plusDays(1).toString(),
                "xp", invalidXp
        );
        givenJson().body(task).when().post(TASKS_ENDPOINT).then().statusCode(400);
    }

    @Test
    void postTask_nullDueDate_returnsCreated() {
        TaskRequestDTO task = buildTaskRequest(null, "Valid Task", DEFAULT_DESCRIPTION, null, LocalDate.now().plusDays(1), 1L);
        postTaskAndReturnLocation(task);
    }

    @Test
    void postTask_validDueDate_returnsCreated() {
        TaskRequestDTO task = buildTaskRequest(null, "Valid Task", DEFAULT_DESCRIPTION, LocalDate.now().plusDays(1), LocalDate.now().plusDays(7), 1L);
        postTaskAndReturnLocation(task);
    }

    @Test
    void postTask_invalidPriorityId_returnsBadRequest() {
        TaskRequestDTO task = buildTaskRequest(null, "Valid Task", DEFAULT_DESCRIPTION, null, LocalDate.now().plusDays(1), 999L);
        givenJson().body(task).when().post(TASKS_ENDPOINT).then().statusCode(400)
                .body(containsString("Priority with ID 999 does not exist"));
    }

    @Test
    void postTask_invalidParentId_returnsBadRequest() {
        TaskRequestDTO task = buildTaskRequest(999L, "Valid Task", DEFAULT_DESCRIPTION, null, LocalDate.now().plusDays(1), 1L);
        givenJson().body(task).when().post(TASKS_ENDPOINT).then().statusCode(400)
                .body(containsString("Parent Task with ID 999 does not exist"));
    }

    @Test
    void postTask_dueDateLaterThanParent_returnsBadRequest() {
        Task parent = saveTask("Parent Task", 10);
        TaskRequestDTO child = buildTaskRequest(parent.getId(), "Child Task", DEFAULT_DESCRIPTION, LocalDate.now().plusDays(11), LocalDate.now().plusDays(11), 1L);
        givenJson().body(child).when().post(TASKS_ENDPOINT).then().statusCode(400)
                .body("detail", containsString("cannot be after parent task's deadline"));
    }

    @Test
    void postTask_deadlineLaterThanParent_returnsBadRequest() {
        Task parent = saveTask("Parent Task", 7);
        TaskRequestDTO child = buildTaskRequest(parent.getId(), "Child Task", DEFAULT_DESCRIPTION, null, LocalDate.now().plusDays(8), 1L);
        givenJson().body(child).when().post(TASKS_ENDPOINT).then().statusCode(400)
                .body(containsString("cannot be after parent task's deadline"));
    }

    @Test
    void postTask_xpLargerThanParent_returnsBadRequest() {
        Priority parentPriority = createPriorityWithXp("HIGHER", (byte) 5);
        Priority childPriority = createPriorityWithXp("VERY_HIGH", (byte) 8);

        Task parent = taskRepository.save(Task.builder()
                .name("Parent Task")
                .description(DEFAULT_DESCRIPTION)
                .deadline(LocalDate.now().plusDays(10))
                .done(false)
                .priority(parentPriority)
                .build());

        TaskRequestDTO child = buildTaskRequest(parent.getId(), "Child Task", DEFAULT_DESCRIPTION, null, LocalDate.now().plusDays(5), childPriority.getId());

        givenJson().body(child).when().post(TASKS_ENDPOINT).then().statusCode(422)
                .body(containsString("cannot exceed parent"));
    }

    @Test
    void postTask_validParentIdAndData_returnsCreated() {
        Task parent = saveTask("Parent Task", 10);
        TaskRequestDTO child = buildTaskRequest(parent.getId(), "Valid Child Task", DEFAULT_DESCRIPTION, LocalDate.now().plusDays(5), LocalDate.now().plusDays(8), 1L);
        postTaskAndReturnLocation(child);

        given().when().get(TASKS_ENDPOINT + "/{id}", parent.getId()).then().statusCode(200)
                .body("name", equalTo("Parent Task"))
                .body("subtasks", equalTo(1));
    }
    // endregion

    // region GET
    @Test
    void getTasks_tasksExist_returnsAllTasks() {
        taskRepository.deleteAll();
        taskRepository.saveAll(List.of(
                createTestTask("Task 1", LocalDate.now().plusDays(1)),
                createTestTask("Task 2", LocalDate.now().plusDays(2))
        ));

        given().when().get(TASKS_ENDPOINT).then().statusCode(200)
                .body("$", hasSize(2))
                .body("name", hasItems("Task 1", "Task 2"));
    }

    @Test
    void getTaskById_validId_returnsTask() {
        Task task = saveTask("Get By Id Task", 3);
        given().when().get(TASKS_ENDPOINT + "/{id}", task.getId()).then().statusCode(200);
        verifyTask(task.getId(), "Get By Id Task", DEFAULT_DESCRIPTION, DEFAULT_XP, false);
    }

    @Test
    void getTaskById_nonExistentId_returnsNotFound() {
        given().when().get(TASKS_ENDPOINT + "/{id}", 999).then().statusCode(404);
    }
    // endregion

    // region PUT
    @Test
    void putTask_validUpdate_returnsOk() {
        Task task = saveTask("Original Task", 1);
        Priority updatedPriority = createPriorityWithXp("Updated Priority", (byte) 5);
        TaskRequestDTO update = buildTaskRequest(null, "Updated Task", "Updated description", null, LocalDate.now().plusDays(5), updatedPriority.getId());

        givenJson().body(update).when().put(TASKS_ENDPOINT + "/{id}", task.getId()).then().statusCode(200);

        verifyTask(task.getId(), "Updated Task", "Updated description", (byte) 5, false);
    }

    @Test
    void putTask_invalidId_returnsNotFound() {
        TaskRequestDTO update = buildTaskRequest(null, "Updated Task", "Updated description", null, LocalDate.now().plusDays(5), 1L);
        givenJson().body(update).when().put(TASKS_ENDPOINT + "/{id}", 999).then().statusCode(404);
    }

    @Test
    void putTask_invalidParentId_returnsBadRequest() {
        Task task = saveTask("Original Task", 1);
        TaskRequestDTO update = buildTaskRequest(999L, "Updated Task", "Updated description", null, LocalDate.now().plusDays(5), 1L);
        givenJson().body(update).when().put(TASKS_ENDPOINT + "/{id}", task.getId()).then().statusCode(400)
                .body(containsString("Parent Task with ID 999 does not exist"));
    }

    @Test
    void putTask_invalidPriorityId_returnsBadRequest() {
        Task task = saveTask("Original Task", 1);
        TaskRequestDTO update = buildTaskRequest(null, "Updated Task", "Updated description", null, LocalDate.now().plusDays(5), 999L);
        givenJson().body(update).when().put(TASKS_ENDPOINT + "/{id}", task.getId()).then().statusCode(400)
                .body(containsString("Priority with ID 999 does not exist"));
    }

    @Test
    void putTask_invalidDueDate_returnsBadRequest() {
        Task task = saveTask("Original Task", 1);
        TaskRequestDTO update = buildTaskRequest(null, "Updated Task", "Updated description", LocalDate.now().minusDays(1), LocalDate.now().plusDays(5), 1L);
        givenJson().body(update).when().put(TASKS_ENDPOINT + "/{id}", task.getId()).then().statusCode(400)
                .body(containsString("Due date must be in the future or present"));
    }

    @Test
    void putTask_invalidDeadline_returnsBadRequest() {
        Task task = saveTask("Original Task", 1);
        TaskRequestDTO update = buildTaskRequest(null, "Updated Task", "Updated description", null, LocalDate.now().minusDays(1), 1L);
        givenJson().body(update).when().put(TASKS_ENDPOINT + "/{id}", task.getId()).then().statusCode(400)
                .body("detail", containsString("Deadline must be in the future or present"));
    }

    @Test
    void putTask_invalidDueDateLaterThanParent_returnsBadRequest() {
        Task parent = saveTask("Parent Task", 10);
        Task task = saveTask("Child Task", 5);
        parent.addSubtask(task);
        taskRepository.save(parent);

        TaskRequestDTO update = buildTaskRequest(parent.getId(), "Updated Child Task", "Updated description", LocalDate.now().plusDays(11), LocalDate.now().plusDays(11), 1L);
        givenJson().body(update).when().put(TASKS_ENDPOINT + "/{id}", task.getId()).then().statusCode(400)
                .body("detail", containsString("cannot be after parent task's deadline"));
    }

    @Test
    void putTask_invalidDeadlineLaterThanParent_returnsBadRequest() {
        Task parent = createTestTask("Parent Task", LocalDate.now().plusDays(11));
        Task task = createTestTask("Child Task", LocalDate.now().plusDays(8));
        parent.addSubtask(task);
        taskRepository.save(parent);

        TaskRequestDTO update = buildTaskRequest(parent.getId(), "Updated Child Task", "Updated description", null, LocalDate.now().plusDays(12), 1L);
        givenJson().body(update).when().put(TASKS_ENDPOINT + "/{id}", task.getId()).then().statusCode(400)
                .body(containsString("cannot be after parent task's deadline"));
    }

    @Test
    void putTask_invalidXpLargerThanParent_returnsBadRequest() {
        Priority parentPriority = createPriorityWithXp("HIGHER", (byte) 5);
        Priority childPriority = createPriorityWithXp("VERY_HIGH", (byte) 8);

        Task parent = taskRepository.save(Task.builder()
                .name("Parent Task")
                .description(DEFAULT_DESCRIPTION)
                .deadline(LocalDate.now().plusDays(10))
                .done(false)
                .priority(parentPriority)
                .build());

        Task task = saveTask("Child Task", 5);
        TaskRequestDTO update = buildTaskRequest(parent.getId(), "Updated Child Task", "Updated description", null, LocalDate.now().plusDays(5), childPriority.getId());

        givenJson().body(update).when().put(TASKS_ENDPOINT + "/{id}", task.getId()).then().statusCode(422)
                .body(containsString("cannot exceed parent"));
    }
    // endregion

    // region PATCH
    @Test
    void patchTaskDone_validId_marksTaskAsDone() {
        Task task = saveTask("Task To Complete", 1);
        given().when().patch(TASKS_ENDPOINT + "/{id}/done", task.getId()).then().statusCode(200);
        verifyTask(task.getId(), null, null, null, true);
    }

    @Test
    void patchTaskDone_invalidId_returnsNotFound() {
        given().when().patch(TASKS_ENDPOINT + "/{id}/done", 999).then().statusCode(404);
    }

    @Test
    void patchTaskDone_parentId_updatesSubtasks() {
        Task parent = saveTask("Parent Task", 1);
        Task child = saveTask("Child Task", 1);
        parent.addSubtask(child);
        taskRepository.save(parent);

        given().when().patch(TASKS_ENDPOINT + "/{id}/done", parent.getId()).then().statusCode(200);

        verifyTask(child.getId(), null, null, null, true);
    }

    @Test
    void markTaskAsDone_insufficientXp_doesNotUpdateStreak() {
        // Given
        User user = createTestUser(10); // Daily quota of 10 XP
        Priority lowPriority = createPriorityWithXp("LOW", (byte) 3);
        Task task = createTaskWithUser("Low XP Task", user, lowPriority);

        // When
        given().when().patch(TASKS_ENDPOINT + "/{id}/done", task.getId()).then().statusCode(200);

        // Then
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getCurrentXp()).isEqualTo(3);
        assertThat(updatedUser.getCurrentStreak()).isEqualTo(0);
        assertThat(updatedUser.getLastAchievedDate()).isNull();
    }

    @Test
    void markTaskAsDone_sufficientXp_updatesStreak() {
        // Given
        User user = createTestUser(5); // Daily quota of 5 XP
        Priority highPriority = createPriorityWithXp("HIGH", (byte) 6);
        Task task = createTaskWithUser("High XP Task", user, highPriority);

        // When
        given().when().patch(TASKS_ENDPOINT + "/{id}/done", task.getId()).then().statusCode(200);

        // Then
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getCurrentXp()).isEqualTo(1); // 6 - 5 = 1, so it wraps around to 1
        assertThat(updatedUser.getCurrentStreak()).isEqualTo(1);
        assertThat(updatedUser.getLastAchievedDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void markTaskAsDone_multipleTasks_accumulatesXp() {
        // Given
        User user = createTestUser(10); // Daily quota of 10 XP
        Priority priority = createPriorityWithXp("MEDIUM", (byte) 4);
        Task task1 = createTaskWithUser("Task 1", user, priority);
        Task task2 = createTaskWithUser("Task 2", user, priority);
        Task task3 = createTaskWithUser("Task 3", user, priority);

        // When
        given().when().patch(TASKS_ENDPOINT + "/{id}/done", task1.getId()).then().statusCode(200);
        given().when().patch(TASKS_ENDPOINT + "/{id}/done", task2.getId()).then().statusCode(200);
        given().when().patch(TASKS_ENDPOINT + "/{id}/done", task3.getId()).then().statusCode(200);

        // Then
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getCurrentXp()).isEqualTo(2);
        assertThat(updatedUser.getCurrentStreak()).isEqualTo(1);
        assertThat(updatedUser.getLastAchievedDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void markTaskAsDone_consecutiveDays_maintainsStreak() {
        // Given
        User user = createTestUser(5);
        Priority priority = createPriorityWithXp("HIGH", (byte) 6);
        Task task1 = createTaskWithUser("Day 1 Task", user, priority);
        Task task2 = createTaskWithUser("Day 2 Task", user, priority);

        // When - Complete task on first day
        given().when().patch(TASKS_ENDPOINT + "/{id}/done", task1.getId()).then().statusCode(200);

        // Then
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getCurrentStreak()).isEqualTo(1);
        assertThat(updatedUser.getLastAchievedDate()).isEqualTo(LocalDate.now());
        updatedUser.setLastAchievedDate(LocalDate.now().minusDays(1));
        userRepository.save(updatedUser);

        // When - Complete task on second day
        given().when().patch(TASKS_ENDPOINT + "/{id}/done", task2.getId()).then().statusCode(200);

        // Then
        updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getCurrentStreak()).isEqualTo(2);
        assertThat(updatedUser.getLastAchievedDate()).isEqualTo(LocalDate.now());
        assertThat(updatedUser.getCurrentXp()).isEqualTo(2);
    }

    @Test
    void markTaskAsDone_skippedDay_resetsStreak() {
        // Given
        User user = createTestUser(5);
        Priority priority = createPriorityWithXp("HIGH", (byte) 6);
        Task task1 = createTaskWithUser("Day 1 Task", user, priority);
        Task task2 = createTaskWithUser("Day 3 Task", user, priority);

        // When - Complete task on first day
        given().when().patch(TASKS_ENDPOINT + "/{id}/done", task1.getId()).then().statusCode(200);

        // Simulate skipping a day
        user.setLastAchievedDate(LocalDate.now().minusDays(2));
        userRepository.save(user);

        // Complete task on third day
        given().when().patch(TASKS_ENDPOINT + "/{id}/done", task2.getId()).then().statusCode(200);

        // Then
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getCurrentStreak()).isEqualTo(1);
        assertThat(updatedUser.getLastAchievedDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void markTaskAsDone_taskWithSubtasks_aggregatesXp() {
        // Given
        User user = createTestUser(10); // Daily quota of 10 XP
        Priority parentPriority = createPriorityWithXp("PARENT", (byte) 5);
        Priority childPriority = createPriorityWithXp("CHILD", (byte) 3);
        Task parentTask = createTaskWithUser("Parent Task", user, parentPriority);
        Task childTask1 = createTaskWithUser("Child Task", user, childPriority);
        Task childTask2 = createTaskWithUser("Child Task", user, childPriority);

        parentTask.addSubtask(childTask1);
        parentTask.addSubtask(childTask2);
        taskRepository.save(parentTask);

        // When
        given().when().patch(TASKS_ENDPOINT + "/{id}/done", parentTask.getId()).then().statusCode(200);

        // Then
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getCurrentXp()).isEqualTo(1);
        assertThat(updatedUser.getCurrentStreak()).isEqualTo(1);
        assertThat(updatedUser.getLastAchievedDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void markTaskAsDone_userHasDayOff_doesNotUpdateXp() {
        // Given
        User user = createTestUser(5); // Daily quota of 5 XP
        user.setOffDays(Set.of(LocalDate.now().getDayOfWeek()));
        user.setCurrentStreak(3);
        user.setLastAchievedDate(LocalDate.now().minusDays(1));
        userRepository.save(user);

        Priority priority = createPriorityWithXp("MEDIUM", (byte) 4);
        Task task = createTaskWithUser("Task on Day Off", user, priority);

        // When
        given().when().patch(TASKS_ENDPOINT + "/{id}/done", task.getId()).then().statusCode(200);

        // Then
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getCurrentXp()).isEqualTo(0);
        assertThat(updatedUser.getCurrentStreak()).isEqualTo(4);
    }

    @Test
    void markTaskAsDone_userHasDayOffAndMissesDailyQuota_keepsStreak() {
        // Given
        User user = createTestUser(10);
        user.setCurrentStreak(5); // User already has a streak of 5 days

        // Set yesterday and the day before as a day off
        DayOfWeek yesterday = LocalDate.now().minusDays(1).getDayOfWeek();
        user.setOffDays(Set.of(yesterday, yesterday.minus(1)));
        user.setDaysOffPerWeek((byte) 2);

        // Last achieved date was 2 days ago (before the day off)
        user.setLastAchievedDate(LocalDate.now().minusDays(3));
        userRepository.save(user);

        Priority priority = createPriorityWithXp("HIGH", (byte) 15);
        Task task = createTaskWithUser("Today's Task", user, priority);

        // When - Complete today's task
        given().when().patch(TASKS_ENDPOINT + "/{id}/done", task.getId()).then().statusCode(200);

        // Then - Streak should be preserved and incremented
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getCurrentStreak()).isEqualTo(6); // Streak is incremented
        assertThat(updatedUser.getLastAchievedDate()).isEqualTo(LocalDate.now());
        assertThat(updatedUser.getCurrentXp()).isEqualTo(5); // 15 - 10 = 5
    }
    // endregion

    // region DELETE
    @Test
    void deleteTask_validId_removesTask() {
        Task task = saveTask("Task To Delete", 1);
        given().when().delete(TASKS_ENDPOINT + "/{id}", task.getId()).then().statusCode(204);
    }

    @Test
    void deleteTask_invalidId_returnsNotFound() {
        given().when().delete(TASKS_ENDPOINT + "/{id}", 999).then().statusCode(404);
    }

    @Test
    void deleteTask_withSubtasks_removesAll() {
        Task parent = saveTask("Parent Task", 1);
        Task child = saveTask("Child Task", 1);
        parent.addSubtask(child);
        taskRepository.save(parent);

        given().when().delete(TASKS_ENDPOINT + "/{id}", parent.getId()).then().statusCode(204);
        given().when().get(TASKS_ENDPOINT + "/{id}", child.getId()).then().statusCode(404);
    }
    // endregion
}
