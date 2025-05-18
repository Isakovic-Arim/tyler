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
import tyler.server.entity.Priority;
import tyler.server.entity.Task;
import tyler.server.entity.User;
import tyler.server.repository.PriorityRepository;
import tyler.server.repository.TaskRepository;
import tyler.server.repository.UserRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

class TaskResourceDeleteTest extends BaseTaskResourceTest {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private JdbcMutableAclService aclService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PriorityRepository priorityRepository;

    private Priority priority;
    private User user;
    private String token;

    @BeforeAll
    void setUp() {
        priority = Priority.builder()
                .name("HIGH")
                .xp((byte) 3)
                .build();
        priorityRepository.save(priority);

        user = User.builder()
                .username("user")
                .passwordHash(passwordEncoder.encode("test"))
                .currentXp(0)
                .dailyXpQuota(0)
                .currentStreak(0)
                .daysOffPerWeek((byte) 2)
                .offDays(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
                .build();
        userRepository.save(user);

        token = getAuthToken(user.getUsername(), "test");
    }

    @AfterEach
    void tearDown() {
        user.getTasks().clear();
        userRepository.save(user);
        taskRepository.deleteAll();
    }

    @AfterAll
    void cleanUp() {
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
    void deleteTask_validId_removesTask() {
        Task task = Task.builder()
                .name("Task To Delete")
                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .user(user)
                .build();
        user.addTask(task);
        task = taskRepository.save(task);
        createAclForTask(task);

        givenToken(token).when().delete(TASKS_ENDPOINT + "/{id}", task.getId()).then().statusCode(204);
    }

    @Test
    @WithMockUser(username = "user")
    void deleteTask_invalidId_returnsNotFound() {
        givenToken(token).when().delete(TASKS_ENDPOINT + "/{id}", 999).then().statusCode(404);
    }

    @Test
    @WithMockUser(username = "user")
    void deleteTask_withSubtasks_removesAll() {
        Task parent = Task.builder()
                .name("Parent Task")
                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .user(user)
                .build();
        user.addTask(parent);
        parent = taskRepository.save(parent);
        createAclForTask(parent);

        Task child = Task.builder()
                .name("Child Task")
                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .user(user)
                .build();
        user.addTask(child);
        child = taskRepository.save(child);
        createAclForTask(child);

        parent.addSubtask(child);
        taskRepository.save(parent);

        givenToken(token).when().delete(TASKS_ENDPOINT + "/{id}", parent.getId()).then().statusCode(204);
        givenToken(token).when().get(TASKS_ENDPOINT + "/{id}", child.getId()).then().statusCode(404);
    }
}