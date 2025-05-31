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
import tyler.server.integration.resource.BaseResourceTest;
import tyler.server.repository.PriorityRepository;
import tyler.server.repository.RefreshTokenRepository;
import tyler.server.repository.TaskRepository;
import tyler.server.repository.UserRepository;

import java.time.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;

class TaskResourcePatchTest extends BaseResourceTest {

    @Autowired
    private PriorityRepository priorityRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private UserRepository userRepository;

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
        user.setDaysOff(new HashSet<>());
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

    protected void verifyTask(Long id, String name, String description, Byte xp, Boolean done) {
        var request = givenCookies(cookies)
                .when()
                .get(TASKS_ENDPOINT + "/{id}", id)
                .then()
                .statusCode(200);
        if (name != null) request.body("name", equalTo(name));
        if (description != null) request.body("description", equalTo(description));
        if (xp != null) request.body("remainingXp", equalTo(xp.intValue()));
        if (done != null) request.body("done", equalTo(done));
    }

    @Test
    @WithMockUser(username = "user")
    void patchTaskDone_validId_marksTaskAsDone() {
        Task task = Task.builder()
                .name("Task To Complete")
                .dueDate(null)
                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .parent(null)
                .user(user)
                .build();
        user.addTask(task);
        task = taskRepository.save(task);
        createAclForTask(task);

        givenCookies(cookies).when().patch(TASKS_ENDPOINT + "/{id}/done", task.getId()).then().statusCode(200);
        verifyTask(task.getId(), null, null, null, true);
    }

    @Test
    @WithMockUser(username = "user")
    void patchTaskDone_invalidId_returnsNotFound() {
        givenCookies(cookies).when().patch(TASKS_ENDPOINT + "/{id}/done", 999).then().statusCode(404);
    }

    @Test
    @WithMockUser(username = "user")
    void patchTaskDone_parentId_updatesSubtasks() {
        Task parent = Task.builder()
                .name("Parent Task")
                .dueDate(null)
                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .parent(null)
                .user(user)
                .build();
        user.addTask(parent);
        parent = taskRepository.save(parent);
        createAclForTask(parent);

        Task child = Task.builder()
                .name("Child Task")
                .dueDate(null)
                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .parent(null)
                .user(user)
                .build();
        user.addTask(child);
        child = taskRepository.save(child);
        createAclForTask(child);

        parent.addSubtask(child);
        taskRepository.save(parent);

        givenCookies(cookies).when().patch(TASKS_ENDPOINT + "/{id}/done", parent.getId()).then().statusCode(200);
        verifyTask(child.getId(), null, null, null, true);
    }

//    @Test
//    @WithMockUser(username = "user")
//    void markTaskAsDone_userHasDayOffAndMissesDailyQuota_keepsStreak() {
//        user.setCurrentStreak(5);
//        user.setDailyXpQuota(5);
//        user.setDaysOff(Set.of(
//                LocalDate.now().plusDays(1),
//                LocalDate.now().plusDays(2)
//        ));
//        user.setDaysOffPerWeek((byte) 2);
//        user.setLastAchievedDate(LocalDate.now().minusDays(3));
//        user.setCurrentXp(1);
//        user = userRepository.save(user);
//
//        Priority priority = Priority.builder().name("HIGH").xp((byte) 5).build();
//        priority = priorityRepository.save(priority);
//
//        Task task = Task.builder()
//                .name("Today's Task")
//                .dueDate(null)
//                .deadline(LocalDate.now().plusDays(1))
//                .done(false)
//                .priority(priority)
//                .parent(null)
//                .user(user)
//                .build();
//        user.addTask(task);
//        task = taskRepository.save(task);
//        createAclForTask(task);
//
//        givenCookies(cookies)
//                .when().patch(TASKS_ENDPOINT + "/{id}/done", task.getId()).then().statusCode(200);
//
//        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
//        assertThat(updatedUser.getCurrentStreak()).isEqualTo(6);
//        assertThat(updatedUser.getLastAchievedDate()).isEqualTo(LocalDate.now());
//        assertThat(updatedUser.getCurrentXp()).isEqualTo(1);
//    }

    @Test
    @WithMockUser(username = "user")
    void markTaskAsDone_completesSubtask_subtractXpOfSubtaskFromParentTask() {
        Task parent = Task.builder()
                .name("Parent Task")
                .dueDate(null)
                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .remainingXp(priority.getXp())
                .parent(null)
                .user(user)
                .build();
        user.addTask(parent);
        parent = taskRepository.save(parent);
        createAclForTask(parent);

        Task child = Task.builder()
                .name("Child Task")
                .dueDate(null)
                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .remainingXp(priority.getXp())
                .parent(null)
                .user(user)
                .build();
        user.addTask(child);
        child = taskRepository.save(child);
        createAclForTask(child);

        parent.addSubtask(child);
        taskRepository.save(parent);

        givenCookies(cookies).when().patch(TASKS_ENDPOINT + "/{id}/done", child.getId()).then().statusCode(200);
        verifyTask(child.getId(), null, null, null, true);
        verifyTask(parent.getId(), null, null, null, false);
    }

    @Test
    @WithMockUser(username = "user")
    void markSubtaskDone_completesParentTask_parentAndChildRemainPresent() {
        Task parent = Task.builder()
                .name("Parent Task")
                .dueDate(null)
                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .remainingXp(priority.getXp())
                .parent(null)
                .user(user)
                .build();
        user.addTask(parent);
        parent = taskRepository.save(parent);
        createAclForTask(parent);

        Task child = Task.builder()
                .name("Child Task")
                .dueDate(null)
                .deadline(LocalDate.now().plusDays(1))
                .done(false)
                .priority(priority)
                .remainingXp(priority.getXp())
                .parent(null)
                .user(user)
                .build();
        user.addTask(child);
        parent.addSubtask(child);
        child = taskRepository.save(child);
        createAclForTask(child);

        givenCookies(cookies)
                .when()
                .patch(TASKS_ENDPOINT + "/{id}/done", child.getId())
                .then()
                .statusCode(200);

        verifyTask(child.getId(), null, null, null, true);
        verifyTask(parent.getId(), null, null, null, false);

        List<Task> tasks = taskRepository.findAllTasks();
        long userTasksCount = tasks.stream().filter(t -> t.getUser().getId().equals(user.getId())).count();
        assertThat(userTasksCount).isEqualTo(2L);
    }
}