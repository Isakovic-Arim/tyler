package tyler.server.unit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import tyler.server.entity.Priority;
import tyler.server.entity.Task;
import tyler.server.entity.User;
import tyler.server.service.ProgressService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

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
                .build();

        task = Task.builder()
                .id(1L)
                .name("Test Task")
                .description("Test Description")
                .priority(priority)
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
}
