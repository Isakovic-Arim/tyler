package tyler.server.unit.entity;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import tyler.server.entity.Task;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static tyler.server.Constants.*;

class TaskTest {
    private static Validator validator;
    private Task task;

    private final LocalDate today = LocalDate.now();
    private final LocalDate tomorrow = today.plusDays(1);
    private final LocalDate nextWeek = today.plusDays(7);

    @BeforeAll
    static void init() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @BeforeEach
    void setUp() {
        task = new Task(
                1L,
                "Test Task",
                "This is a test description",
                tomorrow,
                nextWeek,
                (byte) 50,
                false
        );
    }

    @Test
    void validate_ValidTask_ShouldPass() {
        Set<ConstraintViolation<Task>> violations = validator.validate(task);
        assertTrue(violations.isEmpty());
    }

    @Test
    void validate_NullName_ShouldFail() {
        task.setName(null);
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("name", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validate_EmptyName_ShouldFail() {
        task.setName("");
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        assertFalse(violations.isEmpty());
        assertEquals(2, violations.size());
        assertEquals("name", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validate_NameExceedsMaxLength_ShouldFail() {
        task.setName("a".repeat(MAX_NAME_LENGTH + 1));
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("name", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validate_DescriptionExceedsMaxLength_ShouldFail() {
        task.setDescription("a".repeat(MAX_DESCRIPTION_LENGTH + 1));
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("description", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validate_DueDateInPast_ShouldFail() {
        task.setDueDate(today.minusDays(1));
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("dueDate", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validate_NullDeadline_ShouldFail() {
        task.setDeadline(null);
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("deadline", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validate_DeadlineInPast_ShouldFail() {
        task.setDeadline(today.minusDays(1));
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("deadline", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validate_XpTooLow_ShouldFail() {
        task.setXp((byte) 0);
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("xp", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validate_XpTooHigh_ShouldFail() {
        task.setXp((byte) (MAX_XP + 1));
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("xp", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void setDone_ShouldToggleDoneStatus() {
        assertFalse(task.isDone());
        task.setDone(true);
        assertTrue(task.isDone());
    }
}