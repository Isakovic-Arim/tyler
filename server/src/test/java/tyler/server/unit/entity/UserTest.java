package tyler.server.unit.entity;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tyler.server.entity.Task;
import tyler.server.entity.User;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


class UserTest {
    private static Validator validator;
    private User user;
    private Task task;

    @BeforeAll
    static void init() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @BeforeEach
    void setUp() {
        user = User.builder()
            .id(1L)
            .username("testuser")
            .passwordHash("hashedPassword123")
            .currentXp(100)
            .dailyXpQuota(50)
            .currentStreak(3)
            .lastAchievedDate(LocalDate.now())
            .daysOffPerWeek((byte) 2)
            .offDays(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
            .build();

        task = Task.builder()
            .id(1L)
            .name("Test Task")
            .build();
    }

    @Test
    void validate_ValidUser_ShouldPass() {
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

    @Test
    void validate_NullUsername_ShouldFail() {
        user.setUsername(null);
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertEquals(1, violations.size());
        assertEquals("username", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validate_NullPasswordHash_ShouldFail() {
        user.setPasswordHash(null);
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertEquals(1, violations.size());
        assertEquals("passwordHash", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validate_NegativeCurrentXp_ShouldFail() {
        user.setCurrentXp(-1);
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertEquals(1, violations.size());
        assertEquals("currentXp", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validate_NegativeDailyXpQuota_ShouldFail() {
        user.setDailyXpQuota(-1);
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertEquals(1, violations.size());
        assertEquals("dailyXpQuota", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validate_NegativeCurrentStreak_ShouldFail() {
        user.setCurrentStreak(-1);
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertEquals(1, violations.size());
        assertEquals("currentStreak", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validate_InvalidDaysOffPerWeek_ShouldFail() {
        user.setDaysOffPerWeek((byte) 8);
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertEquals(1, violations.size());
        assertEquals("daysOffPerWeek", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void addTask_ShouldAddTaskAndSetUser() {
        user.addTask(task);
        
        assertTrue(user.getTasks().contains(task));
        assertEquals(user, task.getUser());
    }

    @Test
    void removeTask_ShouldRemoveTaskAndClearUser() {
        user.addTask(task);
        user.removeTask(task);
        
        assertFalse(user.getTasks().contains(task));
        assertNull(task.getUser());
    }

    @Test
    void equals_ShouldWorkCorrectly() {
        User sameUser = User.builder()
            .id(1L)
            .username("different")
            .build();
        
        User differentUser = User.builder()
            .id(2L)
            .username("testuser")
            .build();

        assertEquals(user, sameUser);
        assertNotEquals(user, differentUser);
        assertNotEquals(user, null);
        assertNotEquals(user, new Object());
    }
}
