package tyler.server.integration.resource.task;

import org.aspectj.runtime.internal.Conversions;
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
import tyler.server.entity.Priority;
import tyler.server.entity.Task;
import tyler.server.entity.User;
import tyler.server.repository.PriorityRepository;
import tyler.server.repository.RefreshTokenRepository;
import tyler.server.repository.TaskRepository;
import tyler.server.repository.UserRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;

class TaskResourceGetTest extends BaseTaskResourceTest {

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
            .name("HIGH")
            .xp((byte) 3)
            .build();

    private User user;
    private Map<String, String> cookies;

    @BeforeAll
    void setUp() {
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

    @Test
    @WithMockUser(username = "user")
    void getTasks_tasksExist_returnsAllTasks() {
        Task task = Task.builder()
                .name("Task 1")
                .description("Task 1 description")
                .dueDate(null)
                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .build();
        Task task2 = task.toBuilder().name("Task 2").build();

        user.addTask(task);
        user.addTask(task2);
        taskRepository.saveAll(List.of(task, task2));

        createAcl(task, user);
        createAcl(task2, user);

        givenCookies(cookies)
                .when()
                .get(TASKS_ENDPOINT)
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("name", hasItems("Task 1", "Task 2"));
    }

    @Test
    @WithMockUser(username = "user")
    void getTaskById_validId_returnsTask() {
        Task task = Task.builder()
                .name("Get By Id Task")
                .description("Get By Id Task description")
                .dueDate(null)
                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .user(user)
                .build();
        user.addTask(task);
        task = taskRepository.save(task);
        createAcl(task, user);

        var result = givenCookies(cookies)
                .when()
                .get(TASKS_ENDPOINT + "/{id}", task.getId())
                .then()
                .statusCode(200);

        result.body("id", equalTo(task.getId().intValue()))
                .body("name", equalTo(task.getName()))
                .body("description", equalTo(task.getDescription()))
                .body("xp", equalTo(Conversions.intValue(task.getPriority().getXp())))
                .body("done", equalTo(task.isDone()));
    }

    @Test
    @WithMockUser(username = "user")
    void getTaskById_nonExistentId_returnsNotFound() {
        givenCookies(cookies)
                .when()
                .get(TASKS_ENDPOINT + "/{id}", 999)
                .then()
                .statusCode(404);
    }

    private void createAcl(Task task, User user) {
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
}