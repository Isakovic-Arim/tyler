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
import tyler.server.repository.TaskRepository;
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
    private TaskRepository taskRepository;
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
    void relocateTasksForOffDays_ShouldMoveTasksToDeadline_WhenAllFutureDaysAreOff() {
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfterTomorrow = today.plusDays(2);

        Task task = Task.builder()
                .id(2L)
                .name("Task with future deadline")
                .dueDate(tomorrow)
                .deadline(dayAfterTomorrow)
                .priority(priority)
                .user(user)
                .build();

        user.addTask(task);

        user.setDaysOff(Set.of(today, tomorrow));

        progressService.relocateTasksForOffDays(user);

        assertThat(task.getDueDate()).isEqualTo(dayAfterTomorrow);
    }

    @Test
    void relocateTasksForOffDays_ShouldKeepTaskOnDeadlineDay_WhenDeadlineIsToday() {
        Task task = Task.builder()
                .id(2L)
                .name("Task due today")
                .dueDate(today)
                .deadline(today)
                .priority(priority)
                .user(user)
                .build();

        user.addTask(task);

        // Set today as off day
        user.setDaysOff(Set.of(today));

        progressService.relocateTasksForOffDays(user);

        // Task should stay on today since it's the deadline
        assertThat(task.getDueDate()).isEqualTo(today);
    }

    @Test
    void relocateTasksForOffDays_ShouldHandleSubtasksCorrectly() {
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfterTomorrow = today.plusDays(2);

        Task parentTask = Task.builder()
                .id(2L)
                .name("Parent Task")
                .dueDate(tomorrow)
                .deadline(dayAfterTomorrow)
                .priority(priority)
                .user(user)
                .build();

        Task subtask = Task.builder()
                .id(3L)
                .name("Subtask")
                .dueDate(tomorrow)
                .deadline(dayAfterTomorrow)
                .priority(priority)
                .user(user)
                .build();

        parentTask.addSubtask(subtask);
        user.addTask(parentTask);

        // Set tomorrow as off day
        user.setDaysOff(Set.of(tomorrow));

        progressService.relocateTasksForOffDays(user);

        // Both parent and subtask should be moved to day after tomorrow
        assertThat(parentTask.getDueDate()).isEqualTo(dayAfterTomorrow);
        assertThat(subtask.getDueDate()).isEqualTo(dayAfterTomorrow);
    }

    @Test
    void relocateTasksForOffDays_ShouldNotMoveSubtasksBeyondParentDueDate() {
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfterTomorrow = today.plusDays(2);

        Task parentTask = Task.builder()
                .id(2L)
                .name("Parent Task")
                .dueDate(tomorrow)
                .deadline(dayAfterTomorrow)
                .priority(priority)
                .user(user)
                .build();

        Task subtask = Task.builder()
                .id(3L)
                .name("Subtask")
                .dueDate(tomorrow)
                .deadline(dayAfterTomorrow.plusDays(1)) // Subtask has later deadline
                .priority(priority)
                .user(user)
                .build();

        parentTask.addSubtask(subtask);
        user.addTask(parentTask);

        // Set tomorrow as off day
        user.setDaysOff(Set.of(tomorrow));

        progressService.relocateTasksForOffDays(user);

        // Parent should be moved to day after tomorrow
        assertThat(parentTask.getDueDate()).isEqualTo(dayAfterTomorrow);
        // Subtask should not be moved beyond parent's due date
        assertThat(subtask.getDueDate()).isEqualTo(dayAfterTomorrow);
    }

    @Test
    void relocateTasksForOffDays_ShouldHandleMultipleTasksWithDifferentDeadlines() {
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfterTomorrow = today.plusDays(2);
        LocalDate threeDaysLater = today.plusDays(3);

        Task urgentTask = Task.builder()
                .id(2L)
                .name("Urgent Task")
                .dueDate(tomorrow)
                .deadline(tomorrow)
                .priority(priority)
                .user(user)
                .build();

        Task normalTask = Task.builder()
                .id(3L)
                .name("Normal Task")
                .dueDate(tomorrow)
                .deadline(threeDaysLater)
                .priority(priority)
                .user(user)
                .build();

        user.addTask(urgentTask);
        user.addTask(normalTask);

        // Set tomorrow as off day
        user.setDaysOff(Set.of(tomorrow));

        progressService.relocateTasksForOffDays(user);

        // Urgent task should stay on tomorrow (deadline day)
        assertThat(urgentTask.getDueDate()).isEqualTo(tomorrow);
        // Normal task should be moved to day after tomorrow
        assertThat(normalTask.getDueDate()).isEqualTo(dayAfterTomorrow);
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
        verify(userRepository).saveAll(List.of(user1, user2));
    }

    @Test
    void handleTaskCompletion_ShouldCarryOverExcessXp() {
        user.setCurrentXp(90);
        user.setDailyXpQuota(100);

        progressService.handleTaskCompletion(task);

        // Should have 0 XP after reaching quota (100 - 100 = 0)
        assertThat(user.getCurrentXp()).isEqualTo(0);
        assertThat(user.getCurrentStreak()).isEqualTo(1);
        assertThat(user.getLastAchievedDate()).isEqualTo(today);
    }

    @Test
    void handleTaskCompletion_ShouldCarryOverExcessXpToNextDay() {
        user.setCurrentXp(90);
        user.setDailyXpQuota(50);

        progressService.handleTaskCompletion(task);

        // Should have 50 XP after reaching quota (90 + 10 - 50 = 50)
        assertThat(user.getCurrentXp()).isEqualTo(50);
        assertThat(user.getCurrentStreak()).isEqualTo(1);
        assertThat(user.getLastAchievedDate()).isEqualTo(today);
    }

    @Test
    void checkDailyStreaks_ShouldUpdateStreakForUsersWithEnoughXp() {
        User user1 = User.builder()
                .currentXp(100)
                .dailyXpQuota(50)
                .currentStreak(5)
                .lastAchievedDate(today.minusDays(1))
                .build();

        User user2 = User.builder()
                .currentXp(200)
                .dailyXpQuota(100)
                .currentStreak(3)
                .daysOff(Set.of(today.minusDays(1), today))
                .lastAchievedDate(today.minusDays(2))
                .build();

        when(userRepository.findUsersWithEnoughXpForDailyQuota())
                .thenReturn(List.of(user1, user2));

        progressService.checkDailyStreaks();

        // Both users should have their streaks incremented
        assertThat(user1.getCurrentStreak()).isEqualTo(6);
        assertThat(user2.getCurrentStreak()).isEqualTo(4);
        // XP should be deducted
        assertThat(user1.getCurrentXp()).isEqualTo(50);
        assertThat(user2.getCurrentXp()).isEqualTo(100);
        // Last achieved date should be updated
        assertThat(user1.getLastAchievedDate()).isEqualTo(today);
        assertThat(user2.getLastAchievedDate()).isEqualTo(today);

        verify(userRepository).saveAll(List.of(user1, user2));
    }

    @Test
    void checkDailyStreaks_ShouldNotUpdateStreakForUsersWithInsufficientXp() {
        User user1 = User.builder()
                .currentXp(40)
                .dailyXpQuota(50)
                .currentStreak(5)
                .lastAchievedDate(today.minusDays(1))
                .build();

        User user2 = User.builder()
                .currentXp(90)
                .dailyXpQuota(100)
                .currentStreak(3)
                .lastAchievedDate(today.minusDays(2))
                .build();

        when(userRepository.findUsersWithEnoughXpForDailyQuota())
                .thenReturn(List.of());
        when(userRepository.findUsersWhoAreNotOffAndMissedDailyQuotaToday())
                .thenReturn(List.of(user1, user2));

        progressService.checkDailyStreaks();

        // Both users should have their streaks reset
        assertThat(user1.getCurrentStreak()).isZero();
        assertThat(user2.getCurrentStreak()).isZero();
        // XP should remain unchanged
        assertThat(user1.getCurrentXp()).isEqualTo(40);
        assertThat(user2.getCurrentXp()).isEqualTo(90);

        verify(userRepository).saveAll(List.of(user1, user2));
    }

    @Test
    void checkDailyStreaks_ShouldNotUpdateStreakForUsersOnOffDay() {
        User user = User.builder()
                .currentXp(100)
                .dailyXpQuota(50)
                .currentStreak(5)
                .lastAchievedDate(today.minusDays(1))
                .daysOff(Set.of(today))
                .build();

        when(userRepository.findUsersWithEnoughXpForDailyQuota())
                .thenReturn(List.of());
        when(userRepository.findUsersWhoAreNotOffAndMissedDailyQuotaToday())
                .thenReturn(List.of());

        progressService.checkDailyStreaks();

        // Streak and XP should remain unchanged
        assertThat(user.getCurrentStreak()).isEqualTo(5);
        assertThat(user.getCurrentXp()).isEqualTo(100);
        assertThat(user.getLastAchievedDate()).isEqualTo(today.minusDays(1));
    }

    @Test
    void penalizeForOverDueTasks_ShouldDecrementUserXp() {
        int initialXp = 10;
        User user = User.builder()
                .currentXp(initialXp)
                .build();

        Task task = Task.builder()
                .id(1L)
                .deadline(LocalDate.now().minusDays(1))
                .user(user)
                .build();

        when(taskRepository.findAllTasksOverDeadline()).thenReturn(List.of(task));

        progressService.penalizeForOverDueTasks();

        assertThat(user.getCurrentXp()).isEqualTo(initialXp - 1);
    }
}
