package tyler.server.integration.resource.task;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import tyler.server.dto.task.TaskRequestDTO;
import tyler.server.entity.Priority;
import tyler.server.entity.Task;
import tyler.server.entity.User;
import tyler.server.integration.resource.BaseResourceTest;
import tyler.server.repository.PriorityRepository;
import tyler.server.repository.RefreshTokenRepository;
import tyler.server.repository.TaskRepository;
import tyler.server.repository.UserRepository;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static tyler.server.Constants.MAX_TASK_DESCRIPTION_LENGTH;
import static tyler.server.Constants.MAX_TASK_NAME_LENGTH;

class TaskResourcePostTest extends BaseResourceTest {

    @Autowired
    private PriorityRepository priorityRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private JdbcMutableAclService aclService;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private final Priority priority = Priority.builder()
            .name("MEDIUM")
            .xp((byte) 3)
            .build();

    private User user;
    private Map<String, String> cookies;

    @BeforeAll
    void setup() {
        priorityRepository.save(priority);

        user = User.builder()
                .username("user")
                .passwordHash(passwordEncoder.encode("test"))
                .currentXp(0)
                .dailyXpQuota(10)
                .currentStreak(0)
                .daysOffPerWeek((byte) 2)
                .daysOff(Set.of())
                .build();
        userRepository.save(user);

        cookies = getAuthCookies(user.getUsername(), "test");
    }

    @AfterEach
    void tearDown() {
        user.getTasks().clear();
        userRepository.save(user);
        taskRepository.deleteAll();
    }

    @AfterAll
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.delete(user);
        priorityRepository.delete(priority);
    }

    private void createAclForTask(Task task) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                ObjectIdentity oid = new ObjectIdentityImpl(Task.class, task.getId());
                MutableAcl acl = aclService.createAcl(oid);
                Sid userSid = new PrincipalSid(user.getUsername());

                acl.insertAce(0, BasePermission.READ, userSid, true);
                acl.insertAce(1, BasePermission.WRITE, userSid, true);
                acl.insertAce(2, BasePermission.DELETE, userSid, true);

                aclService.updateAcl(acl);
            }
        });
    }

    @Test
    @WithMockUser(username = "user")
    void postTask_validTask_returnsCreated() {
        TaskRequestDTO task = new TaskRequestDTO(null, "Valid Task", null, null, LocalDate.now().plusDays(1), priority.getId());
        givenCookies(cookies).body(task).when().post(TASKS_ENDPOINT).then().statusCode(201);
    }

    @Test
    @WithMockUser(username = "user")
    void postTask_emptyName_returnsBadRequest() {
        TaskRequestDTO task = new TaskRequestDTO(null, "", null, null, LocalDate.now().plusDays(1), priority.getId());
        givenCookies(cookies).body(task).when().post(TASKS_ENDPOINT).then().statusCode(400);
    }

    @Test
    @WithMockUser(username = "user")
    void postTask_tooLongName_returnsBadRequest() {
        String tooLongName = "a".repeat(MAX_TASK_NAME_LENGTH + 1);
        TaskRequestDTO task = new TaskRequestDTO(null, tooLongName, null, null, LocalDate.now().plusDays(1), priority.getId());
        givenCookies(cookies).body(task).when().post(TASKS_ENDPOINT).then().statusCode(400);
    }

    @Test
    @WithMockUser(username = "user")
    void postTask_tooLongDescription_returnsBadRequest() {
        String tooLongDescription = "a".repeat(MAX_TASK_DESCRIPTION_LENGTH + 1);
        TaskRequestDTO task = new TaskRequestDTO(null, "Valid Name", tooLongDescription, null, LocalDate.now().plusDays(1), priority.getId());
        givenCookies(cookies).body(task).when().post(TASKS_ENDPOINT).then().statusCode(400);
    }

    @Test
    @WithMockUser(username = "user")
    void postTask_pastDeadline_returnsBadRequest() {
        TaskRequestDTO task = new TaskRequestDTO(null, "Valid Name", null, null, LocalDate.now().minusDays(1), priority.getId());
        givenCookies(cookies).body(task).when().post(TASKS_ENDPOINT).then().statusCode(400);
    }

    @Test
    @WithMockUser(username = "user")
    void postTask_nullDeadline_returnsBadRequest() {
        TaskRequestDTO task = new TaskRequestDTO(null, "Valid Name", null, null, null, priority.getId());
        givenCookies(cookies).body(task).when().post(TASKS_ENDPOINT).then().statusCode(400);
    }

    @Test
    @WithMockUser(username = "user")
    void postTask_pastDueDate_returnsBadRequest() {
        TaskRequestDTO task = new TaskRequestDTO(null, "Valid Name", null, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), priority.getId());
        givenCookies(cookies).body(task).when().post(TASKS_ENDPOINT).then().statusCode(400);
    }

    @Test
    @WithMockUser(username = "user")
    void postTask_nullDueDate_returnsCreated() {
        TaskRequestDTO task = new TaskRequestDTO(null, "Valid Task", null, null, LocalDate.now().plusDays(1), priority.getId());
        givenCookies(cookies).body(task).when().post(TASKS_ENDPOINT).then().statusCode(201);
    }

    @Test
    @WithMockUser(username = "user")
    void postTask_validDueDate_returnsCreated() {
        TaskRequestDTO task = new TaskRequestDTO(null, "Valid Task", null, LocalDate.now().plusDays(1), LocalDate.now().plusDays(7), priority.getId());
        givenCookies(cookies).body(task).when().post(TASKS_ENDPOINT).then().statusCode(201);
    }

    @Test
    @WithMockUser(username = "user")
    void postTask_invalidPriorityId_returnsBadRequest() {
        TaskRequestDTO task = new TaskRequestDTO(null, "Valid Task", null, null, LocalDate.now().plusDays(1), 999L);
        givenCookies(cookies).body(task).when().post(TASKS_ENDPOINT).then().statusCode(400)
                .body(containsString("Priority with ID 999 does not exist"));
    }

    @Test
    @WithMockUser(username = "user")
    void postTask_invalidParentId_returnsBadRequest() {
        TaskRequestDTO task = new TaskRequestDTO(999L, "Valid Task", null, null, LocalDate.now().plusDays(1), priority.getId());
        givenCookies(cookies).body(task).when().post(TASKS_ENDPOINT).then().statusCode(400)
                .body(containsString("Parent Task with ID 999 does not exist"));
    }

    @Test
    @WithMockUser(username = "user")
    void postTask_dueDateLaterThanDeadline_returnsBadRequest() {
        TaskRequestDTO task = new TaskRequestDTO(null, "Valid Task", null, LocalDate.now().plusDays(2), LocalDate.now().plusDays(1), priority.getId());
        givenCookies(cookies).body(task).when().post(TASKS_ENDPOINT).then().statusCode(400)
                .body("detail", containsString("Due date cannot be after task's deadline"));
    }

    @Test
    @WithMockUser(username = "user")
    void postTask_dueDateLaterThanParent_returnsBadRequest() {
        Task parent = Task.builder()
                .name("Parent Task")
                .description(null)
                .deadline(LocalDate.now().plusDays(10))
                .priority(priority)
                .user(user)
                .done(false)
                .build();
        user.addTask(parent);
        parent = taskRepository.save(parent);
        createAclForTask(parent);

        TaskRequestDTO child = new TaskRequestDTO(
                parent.getId(),
                "Child Task",
                null,
                LocalDate.now().plusDays(11),
                LocalDate.now().plusDays(11),
                priority.getId()
        );

        givenCookies(cookies).body(child).when().post(TASKS_ENDPOINT).then().statusCode(400)
                .body("detail", containsString("cannot be after parent task's deadline"));
    }

    @Test
    @WithMockUser(username = "user")
    void postTask_deadlineLaterThanParent_returnsBadRequest() {
        Task parent = Task.builder()
                .name("Parent Task")
                .description(null)
                .deadline(LocalDate.now().plusDays(7))
                .priority(priority)
                .user(user)
                .done(false)
                .build();
        user.addTask(parent);
        parent = taskRepository.save(parent);
        createAclForTask(parent);

        TaskRequestDTO child = new TaskRequestDTO(
                parent.getId(),
                "Child Task",
                null,
                null,
                LocalDate.now().plusDays(8),
                priority.getId()
        );

        givenCookies(cookies).body(child).when().post(TASKS_ENDPOINT).then().statusCode(400)
                .body(containsString("cannot be after parent task's deadline"));
    }

    @Test
    @WithMockUser(username = "user")
    void postTask_xpLargerThanParent_returnsBadRequest() {
        Priority parentPriority = priorityRepository.save(Priority.builder().name("HIGHER").xp((byte) 5).build());
        Priority childPriority = priorityRepository.save(Priority.builder().name("VERY_HIGH").xp((byte) 8).build());

        Task parent = Task.builder()
                .name("Parent Task")
                .description(null)
                .deadline(LocalDate.now().plusDays(10))
                .done(false)
                .priority(parentPriority)
                .user(user)
                .build();
        user.addTask(parent);
        parent = taskRepository.save(parent);
        createAclForTask(parent);

        TaskRequestDTO child = new TaskRequestDTO(
                parent.getId(),
                "Child Task",
                null,
                null,
                LocalDate.now().plusDays(5),
                childPriority.getId()
        );

        givenCookies(cookies).body(child).when().post(TASKS_ENDPOINT).then().statusCode(422)
                .body(containsString("cannot exceed parent"));
    }

    @Test
    @WithMockUser(username = "user")
    void postTask_allSubtasksExceedParent_returnsBadRequest() {
        Priority parentPriority = priorityRepository.save(
                Priority.builder().name("PARENT").xp((byte) 10).build()
        );

        Priority childPriority1 = priorityRepository.save(
                Priority.builder().name("CHILD1").xp((byte) 6).build()
        );
        Priority childPriority2 = priorityRepository.save(
                Priority.builder().name("CHILD2").xp((byte) 7).build()
        );

        Task parent = Task.builder()
                .name("Parent Task")
                .deadline(LocalDate.now().plusDays(10))
                .done(false)
                .priority(parentPriority)
                .user(user)
                .build();
        user.addTask(parent);
        parent = taskRepository.save(parent);
        createAclForTask(parent);

        TaskRequestDTO child1 = new TaskRequestDTO(
                parent.getId(),
                "Child Task 1",
                null,
                null,
                LocalDate.now().plusDays(5),
                childPriority1.getId()
        );

        TaskRequestDTO child2 = new TaskRequestDTO(
                parent.getId(),
                "Child Task 2",
                null,
                null,
                LocalDate.now().plusDays(7),
                childPriority2.getId()
        );

        givenCookies(cookies).body(child1).when().post(TASKS_ENDPOINT).then()
                .statusCode(201);

        givenCookies(cookies).body(child2).when().post(TASKS_ENDPOINT).then()
                .statusCode(422)
                .body(containsString("cannot exceed parent"));
    }

    @Test
    @WithMockUser(username = "user")
    void postTask_validParentIdAndData_returnsCreated() {
        Task parent = Task.builder()
                .name("Parent Task")
                .description(null)
                .deadline(LocalDate.now().plusDays(10))
                .priority(priority)
                .user(user)
                .done(false)
                .build();
        user.addTask(parent);
        parent = taskRepository.save(parent);
        createAclForTask(parent);

        TaskRequestDTO child = new TaskRequestDTO(
                parent.getId(),
                "Valid Child Task",
                null,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(8),
                priority.getId()
        );

        givenCookies(cookies).body(child).when().post(TASKS_ENDPOINT).then().statusCode(201);

        givenCookies(cookies).when().get(TASKS_ENDPOINT + "/{id}", parent.getId()).then().statusCode(200)
                .body("name", equalTo("Parent Task"))
                .body("subtasks", equalTo(1));
    }
}