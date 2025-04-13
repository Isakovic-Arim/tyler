package tyler.server.integration.resource;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Testcontainers;
import tyler.server.entity.dto.TaskDTO;
import tyler.server.repository.TaskRepository;
import tyler.server.entity.Task;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static tyler.server.Constants.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class TaskResourceTest {
    private static final String TASKS_ENDPOINT = "/tasks";
    private static final String DEFAULT_DESCRIPTION = "Test description";
    private static final byte DEFAULT_XP = 10;

    @Container
    @ServiceConnection
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16.8-alpine3.20");

    @Autowired
    private TaskRepository taskRepository;

    @BeforeAll
    static void setUp(@Value("${local.server.port}") int port) {
        RestAssured.port = port;
    }

    @BeforeEach
    void cleanUp() {
        taskRepository.deleteAll();
    }

    private RequestSpecification givenJson() {
        return given().contentType(ContentType.JSON);
    }

    // region POST
    @Test
    void postTask_validTask_returnsCreated() {
        TaskDTO task = new TaskDTO(
                null,
                "Valid Task",
                DEFAULT_DESCRIPTION,
                null,
                LocalDate.now().plusDays(1),
                DEFAULT_XP,
                false
        );

        givenJson()
                .body(task)
                .when()
                .post(TASKS_ENDPOINT)
                .then()
                .statusCode(201)
                .header("Location", containsString(TASKS_ENDPOINT));
    }

    @Test
    void postTask_emptyName_returnsBadRequest() {
        TaskDTO task = new TaskDTO(
                null,
                "",
                DEFAULT_DESCRIPTION,
                null,
                LocalDate.now().plusDays(1),
                DEFAULT_XP,
                false
        );

        givenJson()
                .body(task)
                .when()
                .post(TASKS_ENDPOINT)
                .then()
                .statusCode(400);
    }

    @Test
    void postTask_nullName_returnsBadRequest() {
        Map<String, Object> task = new HashMap<>();
        task.put("description", DEFAULT_DESCRIPTION);
        task.put("deadline", LocalDate.now().plusDays(1).toString());
        task.put("xp", DEFAULT_XP);

        givenJson()
                .body(task)
                .when()
                .post(TASKS_ENDPOINT)
                .then()
                .statusCode(400);
    }

    @Test
    void postTask_tooLongName_returnsBadRequest() {
        TaskDTO task = new TaskDTO(
                null,
                "a".repeat(MAX_NAME_LENGTH + 1),
                DEFAULT_DESCRIPTION,
                null,
                LocalDate.now().plusDays(1),
                DEFAULT_XP,
                false
        );

        givenJson()
                .body(task)
                .when()
                .post(TASKS_ENDPOINT)
                .then()
                .statusCode(400);
    }

    @Test
    void postTask_tooLongDescription_returnsBadRequest() {
        TaskDTO task = new TaskDTO(
                null,
                "Valid Name",
                "a".repeat(MAX_DESCRIPTION_LENGTH + 1),
                null,
                LocalDate.now().plusDays(1),
                DEFAULT_XP,
                false
        );

        givenJson()
                .body(task)
                .when()
                .post(TASKS_ENDPOINT)
                .then()
                .statusCode(400);
    }

    @Test
    void postTask_pastDeadline_returnsBadRequest() {
        TaskDTO task = new TaskDTO(
                null,
                "Valid Name",
                DEFAULT_DESCRIPTION,
                null,
                LocalDate.now().minusDays(1),
                DEFAULT_XP,
                false
        );

        givenJson()
                .body(task)
                .when()
                .post(TASKS_ENDPOINT)
                .then()
                .statusCode(400);
    }

    @Test
    void postTask_nullDeadline_returnsBadRequest() {
        TaskDTO task = new TaskDTO(
                null,
                "Valid Name",
                DEFAULT_DESCRIPTION,
                null,
                null,
                DEFAULT_XP,
                false
        );

        givenJson()
                .body(task)
                .when()
                .post(TASKS_ENDPOINT)
                .then()
                .statusCode(400);
    }

    @Test
    void postTask_pastDueDate_returnsBadRequest() {
        Map<String, Object> task = new HashMap<>();
        task.put("name", "Valid Name");
        task.put("description", DEFAULT_DESCRIPTION);
        task.put("dueDate", LocalDate.now().minusDays(1).toString());
        task.put("deadline", LocalDate.now().plusDays(1).toString());
        task.put("xp", DEFAULT_XP);
        task.put("done", false);

        givenJson()
                .body(task)
                .when()
                .post(TASKS_ENDPOINT)
                .then()
                .statusCode(400);
    }

    @Test
    void postTask_tooLowXp_returnsBadRequest() {
        Map<String, Object> task = new HashMap<>();
        task.put("name", "Valid Name");
        task.put("description", DEFAULT_DESCRIPTION);
        task.put("deadline", LocalDate.now().plusDays(1).toString());
        task.put("xp", 0);

        givenJson()
                .body(task)
                .when()
                .post(TASKS_ENDPOINT)
                .then()
                .statusCode(400);
    }

    @Test
    void postTask_tooHighXp_returnsBadRequest() {
        Map<String, Object> task = new HashMap<>();
        task.put("name", "Valid Name");
        task.put("description", DEFAULT_DESCRIPTION);
        task.put("deadline", LocalDate.now().plusDays(1).toString());
        task.put("xp", MAX_XP + 1);

        givenJson()
                .body(task)
                .when()
                .post(TASKS_ENDPOINT)
                .then()
                .statusCode(400);
    }

    @Test
    void postTask_nullDueDate_returnsCreated() {
        TaskDTO task = new TaskDTO(
                null,
                "Valid Task",
                DEFAULT_DESCRIPTION,
                null,
                LocalDate.now().plusDays(1),
                DEFAULT_XP,
                false
        );

        givenJson()
                .body(task)
                .when()
                .post(TASKS_ENDPOINT)
                .then()
                .statusCode(201)
                .header("Location", containsString(TASKS_ENDPOINT + "/"));
    }

    @Test
    void postTask_validDueDate_returnsCreated() {
        TaskDTO task = new TaskDTO(
                null,
                "Valid Task",
                DEFAULT_DESCRIPTION,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(7),
                DEFAULT_XP,
                false
        );

        givenJson()
                .body(task)
                .when()
                .post(TASKS_ENDPOINT)
                .then()
                .statusCode(201)
                .header("Location", containsString(TASKS_ENDPOINT + "/"));
    }
    // endregion

    // region GET
    @Test
    void getTasks_tasksExist_returnsAllTasks() {
        var task1 = createTestTask("Task 1", LocalDate.now().plusDays(1));
        var task2 = createTestTask("Task 2", LocalDate.now().plusDays(2));

        taskRepository.saveAll(List.of(task1, task2));

        given()
                .when()
                .get(TASKS_ENDPOINT)
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("name", hasItems("Task 1", "Task 2"));
    }

    @Test
    void getTaskById_validId_returnsTask() {
        Task task = createAndSaveTask("Get By Id Task", LocalDate.now().plusDays(3));

        given()
                .when()
                .get(TASKS_ENDPOINT + "/{id}", task.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(task.getId().intValue()))
                .body("name", equalTo("Get By Id Task"))
                .body("description", equalTo(DEFAULT_DESCRIPTION))
                .body("xp", equalTo((int) DEFAULT_XP))
                .body("done", equalTo(false));
    }

    @Test
    void getTaskById_nonExistentId_returnsNotFound() {
        given()
                .when()
                .get(TASKS_ENDPOINT + "/{id}", 999)
                .then()
                .statusCode(404);
    }
    // endregion

    // region PUT
    @Test
    void putTask_validUpdate_returnsOk() {
        Task task = createAndSaveTask("Original Task", LocalDate.now().plusDays(1));
        LocalDate newDeadline = LocalDate.now().plusDays(5);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", "Updated Task");
        updates.put("description", "Updated description");
        updates.put("deadline", newDeadline.toString());
        updates.put("xp", 25);

        givenJson()
                .body(updates)
                .when()
                .put(TASKS_ENDPOINT + "/{id}", task.getId())
                .then()
                .statusCode(200);

        verifyTask(task.getId(),
                "Updated Task",
                "Updated description",
                25,
                false);
    }

    @Test
    void putTask_invalidId_returnsNotFound() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", "Updated Task");
        updates.put("description", "Updated description");
        updates.put("deadline", LocalDate.now().plusDays(5).toString());
        updates.put("xp", 25);

        givenJson()
                .body(updates)
                .when()
                .put(TASKS_ENDPOINT + "/{id}", 999)
                .then()
                .statusCode(404);
    }
    // endregion

    // region PATCH
    @Test
    void patchTaskDone_validId_marksTaskAsDone() {
        Task task = createAndSaveTask("Task To Complete", LocalDate.now().plusDays(1));

        given()
                .when()
                .patch(TASKS_ENDPOINT + "/{id}/done", task.getId())
                .then()
                .statusCode(200);

        verifyTask(task.getId(), null, null, null, true);
    }

    @Test
    void patchTaskDone_invalidId_returnsNotFound() {
        given()
                .when()
                .patch(TASKS_ENDPOINT + "/{id}/done", 999)
                .then()
                .statusCode(404);
    }
    // endregion

    // region DELETE
    @Test
    void deleteTask_validId_removesTask() {
        Task task = createAndSaveTask("Task To Delete", LocalDate.now().plusDays(1));

        given()
                .when()
                .delete(TASKS_ENDPOINT + "/{id}", task.getId())
                .then()
                .statusCode(204);
    }

    @Test
    void deleteTask_invalidId_returnsNotFound() {
        given()
                .when()
                .delete(TASKS_ENDPOINT + "/{id}", 999)
                .then()
                .statusCode(404);
    }
    // endregion

    private Task createAndSaveTask(String name, LocalDate deadline) {
        Task task = createTestTask(name, deadline);
        return taskRepository.save(task);
    }

    private Task createTestTask(String name, LocalDate deadline) {
        return new Task(
                null,
                name,
                DEFAULT_DESCRIPTION,
                null,
                deadline,
                DEFAULT_XP,
                false
        );
    }

    private void verifyTask(Long id, String name, String description, Integer xp, Boolean done) {
        var request = given().when().get(TASKS_ENDPOINT + "/{id}", id).then().statusCode(200);

        if (name != null) request.body("name", equalTo(name));
        if (description != null) request.body("description", equalTo(description));
        if (xp != null) request.body("xp", equalTo(xp));
        if (done != null) request.body("done", equalTo(done));
    }
}