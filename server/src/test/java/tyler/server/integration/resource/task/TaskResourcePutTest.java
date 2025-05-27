package tyler.server.integration.resource.task;

import jakarta.persistence.EntityManagerFactory;
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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

class TaskResourcePutTest extends BaseResourceTest {

    @Autowired
    private PriorityRepository priorityRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;
    
    @Autowired
    private JdbcMutableAclService aclService;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private final Priority priority = Priority.builder()
            .name("HIGH")
            .xp((byte) 5)
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
                .dailyXpQuota(0)
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
    void putTask_validUpdate_returnsOk() {
        Task task = Task.builder()
                .name("Original Task")
                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .parent(null)
                .user(user)
                .build();
        user.addTask(task);
        task = taskRepository.save(task);
        createAclForTask(task);

        Priority updatedPriority = Priority.builder()
                .name("Updated Priority")
                .xp((byte) 5)
                .build();
        updatedPriority = priorityRepository.save(updatedPriority);

        TaskRequestDTO update = new TaskRequestDTO(null, "Updated Task", "Updated description", null,
                LocalDate.now().plusDays(5), updatedPriority.getId());

        givenCookies(cookies).body(update).when().put(TASKS_ENDPOINT + "/{id}", task.getId()).then().statusCode(200);

        givenCookies(cookies).when().get(TASKS_ENDPOINT + "/{id}", task.getId()).then().statusCode(200)
                .body("name", equalTo("Updated Task"))
                .body("description", equalTo("Updated description"))
                .body("xp", equalTo(Conversions.intValue(5)))
                .body("deadline", equalTo(LocalDate.now().plusDays(5).toString()))
                .body("done", equalTo(false));
    }

    @Test
    @WithMockUser(username = "user")
    void putTask_invalidId_returnsNotFound() {
        TaskRequestDTO update = new TaskRequestDTO(null, "Updated Task", "Updated description",
                null, LocalDate.now().plusDays(5), priority.getId());
        givenCookies(cookies).body(update).when().put(TASKS_ENDPOINT + "/{id}", 999).then().statusCode(404);
    }

    @Test
    @WithMockUser(username = "user")
    void putTask_invalidParentId_returnsBadRequest() {
        Task task = Task.builder()
                .name("Original Task")

                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .parent(null)
                .user(user)
                .build();
        user.addTask(task);
        task = taskRepository.save(task);
        createAclForTask(task);

        TaskRequestDTO update = new TaskRequestDTO(999L, "Updated Task", "Updated description",
                null, LocalDate.now().plusDays(5), priority.getId());

        givenCookies(cookies).body(update).when().put(TASKS_ENDPOINT + "/{id}", task.getId())
                .then().statusCode(400)
                .body("detail", containsString("Parent Task with ID 999 does not exist"));
    }

    @Test
    @WithMockUser(username = "user")
    void putTask_shouldPreserveSubtasks() {
        Task parent = Task.builder()
                .name("Parent Task")
                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .user(user)
                .build();
        Task subtask = Task.builder()
                .name("Subtask")
                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .user(user)
                .build();
        parent.addSubtask(subtask);
        user.addTask(parent);
        parent = taskRepository.save(parent);
        createAclForTask(parent);

        TaskRequestDTO update = new TaskRequestDTO(null, "Updated Parent Task", "Updated Parent Description",
                null, LocalDate.now().plusDays(2), priority.getId());

        givenCookies(cookies).body(update)
            .when().put(TASKS_ENDPOINT + "/{id}", parent.getId())
            .then().statusCode(200);

        Task updatedParent = entityManagerFactory.createEntityManager().createQuery("SELECT t FROM Task t JOIN t.subtasks WHERE t.id = :id", Task.class)
                .setParameter("id", parent.getId())
                .getSingleResult();

        assertThat(updatedParent.getSubtasks()).hasSize(1);
        assertThat(updatedParent.getName()).isEqualTo("Updated Parent Task");
    }

    @Test
    @WithMockUser(username = "user")
    void putTask_invalidPriorityId_returnsBadRequest() {
        Task task = Task.builder()
                .name("Original Task")

                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .parent(null)
                .user(user)
                .build();
        user.addTask(task);
        task = taskRepository.save(task);
        createAclForTask(task);

        TaskRequestDTO update = new TaskRequestDTO(null, "Updated Task", "Updated description",
                null, LocalDate.now().plusDays(5), 999L);

        givenCookies(cookies).body(update).when().put(TASKS_ENDPOINT + "/{id}", task.getId())
                .then().statusCode(400)
                .body(containsString("Priority with ID 999 does not exist"));
    }

    @Test
    @WithMockUser(username = "user")
    void putTask_invalidDueDate_returnsBadRequest() {
        Task task = Task.builder()
                .name("Original Task")

                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .parent(null)
                .user(user)
                .build();
        user.addTask(task);
        task = taskRepository.save(task);
        createAclForTask(task);

        TaskRequestDTO update = new TaskRequestDTO(null, "Updated Task", "Updated description",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(5), priority.getId());

        givenCookies(cookies).body(update).when().put(TASKS_ENDPOINT + "/{id}", task.getId())
                .then().statusCode(400)
                .body(containsString("Due date must be in the future or present"));
    }

    @Test
    @WithMockUser(username = "user")
    void putTask_invalidDeadline_returnsBadRequest() {
        Task task = Task.builder()
                .name("Original Task")

                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .parent(null)
                .user(user)
                .build();
        user.addTask(task);
        task = taskRepository.save(task);
        createAclForTask(task);

        TaskRequestDTO update = new TaskRequestDTO(null, "Updated Task", "Updated description",
                null, LocalDate.now().minusDays(1), priority.getId());

        givenCookies(cookies).body(update).when().put(TASKS_ENDPOINT + "/{id}", task.getId())
                .then().statusCode(400)
                .body("detail", containsString("Deadline must be in the future or present"));
    }

    @Test
    @WithMockUser(username = "user")
    void putTask_invalidDueDateLaterThanParent_returnsBadRequest() {
        Task parent = Task.builder()
                .name("Parent Task")

                .deadline(LocalDate.now().plusDays(10))
                .done(false)
                .priority(priority)
                .user(user)
                .build();
        user.addTask(parent);
        parent = taskRepository.save(parent);
        createAclForTask(parent);

        Task child = Task.builder()
                .name("Child Task")

                .deadline(LocalDate.now().plusDays(5))
                .done(false)
                .priority(priority)
                .user(user)
                .build();
        user.addTask(child);
        child = taskRepository.save(child);
        createAclForTask(child);

        parent.addSubtask(child);
        taskRepository.save(parent);

        TaskRequestDTO update = new TaskRequestDTO(parent.getId(), "Updated Child Task", "Updated description",
                LocalDate.now().plusDays(11), LocalDate.now().plusDays(11), priority.getId());

        givenCookies(cookies).body(update).when().put(TASKS_ENDPOINT + "/{id}", child.getId())
                .then().statusCode(400)
                .body("detail", containsString("cannot be after parent task's deadline"));
    }

    @Test
    @WithMockUser(username = "user")
    void putTask_invalidDeadlineLaterThanParent_returnsBadRequest() {
        Task parent = Task.builder()
                .name("Parent Task")
                .deadline(LocalDate.now().plusDays(11))
                .done(false)
                .priority(priority)
                .user(user)
                .build();
        user.addTask(parent);
        parent = taskRepository.save(parent);
        createAclForTask(parent);

        Task child = Task.builder()
                .name("Child Task")
                .deadline(LocalDate.now().plusDays(8))
                .done(false)
                .priority(priority)
                .user(user)
                .build();
        user.addTask(child);
        child = taskRepository.save(child);
        createAclForTask(child);

        parent.addSubtask(child);
        taskRepository.save(parent);

        TaskRequestDTO update = new TaskRequestDTO(parent.getId(), "Updated Child Task", "Updated description",
                null, LocalDate.now().plusDays(12), priority.getId());

        givenCookies(cookies).body(update).when().put(TASKS_ENDPOINT + "/{id}", child.getId())
                .then().statusCode(400)
                .body("detail", containsString("cannot be after parent task's deadline"));
    }

    @Test
    @WithMockUser(username = "user")
    void putTask_invalidXpLargerThanParent_returnsBadRequest() {
        Priority parentPriority = Priority.builder().name("HIGHER").xp((byte) 5).build();
        parentPriority = priorityRepository.save(parentPriority);

        Priority childPriority = Priority.builder().name("VERY_HIGH").xp((byte) 8).build();
        childPriority = priorityRepository.save(childPriority);

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

        Task child = Task.builder()
                .name("Child Task")
                .deadline(LocalDate.now().plusDays(5))
                .done(false)
                .priority(childPriority)
                .user(user)
                .build();
        user.addTask(child);
        child = taskRepository.save(child);
        createAclForTask(child);

        parent.addSubtask(child);
        taskRepository.save(parent);

        TaskRequestDTO update = new TaskRequestDTO(parent.getId(), "Updated Child Task", "Updated description",
                null, LocalDate.now().plusDays(5), childPriority.getId());

        givenCookies(cookies).body(update).when().put(TASKS_ENDPOINT + "/{id}", child.getId())
                .then().statusCode(422)
                .body(containsString("cannot exceed parent"));
    }
}