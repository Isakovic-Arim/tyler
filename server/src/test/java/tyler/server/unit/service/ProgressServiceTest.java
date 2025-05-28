package tyler.server.unit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tyler.server.entity.Priority;
import tyler.server.entity.Task;
import tyler.server.entity.User;
import tyler.server.repository.UserRepository;
import tyler.server.service.ProgressService;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProgressService progressService;

    private User user;
    private Task task;
    private Priority priority;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        priority = Priority.builder()
                .id(1L)
                .name("HIGH")
                .xp((byte) 10)
                .build();

        user = User.builder()
                .id(1L)
                .currentXp(0)
                .dailyXpQuota(100)
                .currentStreak(0)
                .lastAchievedDate(null)
                .daysOff(new HashSet<>())
                .build();

        task = Task.builder()
                .id(1L)
                .name("Test Task")
                .description("Test Description")
                .priority(priority)
                .remainingXp(priority.getXp())
                .user(user)
                .build();

        user.addTask(task);
    }

    @Test
    void handleTaskCompletion_ShouldAddXpToUser() {
        progressService.handleTaskCompletion(task);

        assertThat(user.getCurrentXp()).isEqualTo(10);
        assertThat(user.getCurrentStreak()).isZero();
        assertThat(user.getLastAchievedDate()).isNull();
    }

    @Test
    void handleTaskCompletion_ShouldNotModifyUser_WhenUserIsNull() {
        Task taskWithoutUser = task.toBuilder().user(null).build();

        progressService.handleTaskCompletion(taskWithoutUser);

        assertThat(user.getCurrentXp()).isZero();
        assertThat(user.getCurrentStreak()).isZero();
        assertThat(user.getLastAchievedDate()).isNull();
    }

    @Test
    void handleTaskCompletion_ShouldIncrementStreak_WhenDailyQuotaReached() {
        user.setCurrentXp(90); // Just below daily quota

        progressService.handleTaskCompletion(task);

        assertThat(user.getCurrentXp()).isEqualTo(0); // Reset after quota reached
        assertThat(user.getCurrentStreak()).isEqualTo(1);
        assertThat(user.getLastAchievedDate()).isEqualTo(today);
    }

    @Test
    void handleTaskCompletion_ShouldResetStreak_WhenQuotaReachedAfterGap() {
        user.setCurrentXp(90);
        user.setLastAchievedDate(today.minusDays(2)); // Gap in streak

        progressService.handleTaskCompletion(task);

        assertThat(user.getCurrentXp()).isEqualTo(0);
        assertThat(user.getCurrentStreak()).isEqualTo(1); // Reset to 1
        assertThat(user.getLastAchievedDate()).isEqualTo(today);
    }

    @Test
    void handleTaskCompletion_ShouldNotIncrementStreak_WhenQuotaReachedSameDay() {
        user.setCurrentXp(90);
        user.setLastAchievedDate(today);

        progressService.handleTaskCompletion(task);

        assertThat(user.getCurrentXp()).isEqualTo(100);
        assertThat(user.getCurrentStreak()).isZero();
        assertThat(user.getLastAchievedDate()).isEqualTo(today);
    }

    @Test
    void handleTaskCompletion_ShouldMaintainStreak_WhenQuotaReachedConsecutiveDays() {
        user.setCurrentXp(90);
        user.setLastAchievedDate(today.minusDays(1));
        user.setCurrentStreak(5);

        progressService.handleTaskCompletion(task);

        assertThat(user.getCurrentXp()).isEqualTo(0);
        assertThat(user.getCurrentStreak()).isEqualTo(6);
        assertThat(user.getLastAchievedDate()).isEqualTo(today);
    }

    @Test
    void handleTaskCompletion_ShouldSkipXpOnOffDay() {
        user.setDaysOff(Set.of(today));

        progressService.handleTaskCompletion(task);

        assertThat(user.getCurrentXp()).isZero();
        assertThat(user.getCurrentStreak()).isEqualTo(1);
        assertThat(user.getLastAchievedDate()).isEqualTo(today);
    }

    @Test
    void handleTaskCompletion_ShouldAddXpOnNonOffDay() {
        user.setDaysOff(Set.of(today.plusDays(1))); // Set tomorrow as off day

        progressService.handleTaskCompletion(task);

        assertThat(user.getCurrentXp()).isEqualTo(10);
        assertThat(user.getCurrentStreak()).isZero();
        assertThat(user.getLastAchievedDate()).isNull();
    }

    @Test
    void relocateTasksForOffDays_ShouldRelocateTasksOnOffDays() {
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfterTomorrow = today.plusDays(2);

        Task task1 = Task.builder()
                .id(2L)
                .name("Task 1")
                .dueDate(tomorrow)
                .deadline(dayAfterTomorrow)
                .priority(priority)
                .user(user)
                .build();

        Task task2 = Task.builder()
                .id(3L)
                .name("Task 2")
                .dueDate(dayAfterTomorrow)
                .deadline(dayAfterTomorrow.plusDays(1))
                .priority(priority)
                .user(user)
                .build();

        user.addTask(task1);
        user.addTask(task2);

        // Set tomorrow as off day
        user.setDaysOff(Set.of(tomorrow));

        progressService.relocateTasksForOffDays(user);

        // Task1 should be moved to day after tomorrow
        assertThat(task1.getDueDate()).isEqualTo(dayAfterTomorrow);
        // Task2 should remain unchanged
        assertThat(task2.getDueDate()).isEqualTo(dayAfterTomorrow);
    }

    @Test
    void relocateTasksForOffDays_ShouldNotRelocateTasksBeyondDeadline() {
        LocalDate tomorrow = today.plusDays(1);

        Task task = Task.builder()
                .id(2L)
                .name("Task with tight deadline")
                .dueDate(tomorrow)
                .deadline(tomorrow) // Same day deadline
                .priority(priority)
                .user(user)
                .build();

        user.addTask(task);

        // Set tomorrow as off day
        user.setDaysOff(Set.of(tomorrow));

        progressService.relocateTasksForOffDays(user);

        // Task should not be moved since it would exceed deadline
        assertThat(task.getDueDate()).isEqualTo(tomorrow);
    }

    @Test
    void relocateTasksForOffDays_ShouldHandleMultipleOffDays() {
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfterTomorrow = today.plusDays(2);
        LocalDate threeDaysLater = today.plusDays(3);

        Task task = Task.builder()
                .id(2L)
                .name("Task spanning multiple off days")
                .dueDate(tomorrow)
                .deadline(threeDaysLater)
                .priority(priority)
                .user(user)
                .build();

        user.addTask(task);

        // Set tomorrow and day after tomorrow as off days
        user.setDaysOff(Set.of(
                tomorrow,
                tomorrow.plusDays(1)
        ));

        progressService.relocateTasksForOffDays(user);

        // Task should be moved to three days later
        assertThat(task.getDueDate()).isEqualTo(threeDaysLater);
    }

    @Test
    void relocateTasksForOffDays_ShouldNotRelocateCompletedTasks() {
        LocalDate tomorrow = today.plusDays(1);

        Task completedTask = Task.builder()
                .id(2L)
                .name("Completed Task")
                .dueDate(tomorrow)
                .deadline(tomorrow.plusDays(1))
                .priority(priority)
                .user(user)
                .done(true)
                .build();

        user.addTask(completedTask);

        // Set tomorrow as off day
        user.setDaysOff(Set.of(tomorrow));

        progressService.relocateTasksForOffDays(user);

        // Completed task should not be moved
        assertThat(completedTask.getDueDate()).isEqualTo(tomorrow);
    }

    @Test
    void checkDailyStreaks_shouldResetStreakForEligibleUsers() {
        User user1 = new User();
        user1.setCurrentStreak(5);
        User user2 = new User();
        user2.setCurrentStreak(3);

        when(userRepository.findUsersWhoAreNotOffAndMissedDailyQuotaToday())
                .thenReturn(List.of(user1, user2));

        progressService.checkDailyStreaks();

        assertThat(user1.getCurrentStreak()).isZero();
        assertThat(user2.getCurrentStreak()).isZero();
        verify(userRepository).save(user1);
        verify(userRepository).save(user2);
    }
}
