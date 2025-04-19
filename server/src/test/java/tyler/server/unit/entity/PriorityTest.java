package tyler.server.unit.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tyler.server.entity.Priority;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static tyler.server.Constants.MAX_PRIORITY_NAME_LENGTH;
import static tyler.server.Constants.MAX_PRIORITY_XP;

public class PriorityTest {
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validPriority_shouldHaveNoViolations() {
        Priority priority = Priority.builder()
                .name("HIGH")
                .xp((byte) 10)
                .build();

        Set<ConstraintViolation<Priority>> violations = validator.validate(priority);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullName_shouldHaveViolation() {

        Priority priority = Priority.builder()
                .name(null)
                .xp((byte) 10)
                .build();

        Set<ConstraintViolation<Priority>> violations = validator.validate(priority);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("cannot be blank");
    }

    @Test
    void emptyName_shouldHaveViolation() {

        Priority priority = Priority.builder()
                .name("")
                .xp((byte) 10)
                .build();

        Set<ConstraintViolation<Priority>> violations = validator.validate(priority);

        assertThat(violations).hasSize(2);
    }

    @Test
    void blankName_shouldHaveViolation() {

        Priority priority = Priority.builder()
                .name("   ")
                .xp((byte) 10)
                .build();

        Set<ConstraintViolation<Priority>> violations = validator.validate(priority);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("cannot be blank");
    }

    @Test
    void nameTooShort_shouldHaveViolation() {

        Priority priority = Priority.builder()
                .name("AB")
                .xp((byte) 10)
                .build();

        Set<ConstraintViolation<Priority>> violations = validator.validate(priority);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("between 3 and");
    }

    @Test
    void nameTooLong_shouldHaveViolation() {
        String tooLongName = "A".repeat(MAX_PRIORITY_NAME_LENGTH + 1);
        Priority priority = Priority.builder()
                .name(tooLongName)
                .xp((byte) 10)
                .build();

        Set<ConstraintViolation<Priority>> violations = validator.validate(priority);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("between 3 and");
    }

    @Test
    void xpTooSmall_shouldHaveViolation() {
        Priority priority = Priority.builder()
                .name("HIGH")
                .xp((byte) 0)
                .build();

        Set<ConstraintViolation<Priority>> violations = validator.validate(priority);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("at least 1");
    }

    @Test
    void xpTooLarge_shouldHaveViolation() {
        Priority priority = Priority.builder()
                .name("HIGH")
                .xp((byte) (MAX_PRIORITY_XP + 1))
                .build();

        Set<ConstraintViolation<Priority>> violations = validator.validate(priority);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("cannot exceed");
    }

    @ParameterizedTest
    @ValueSource(bytes = {1, 10, MAX_PRIORITY_XP})
    void validXpValues_shouldHaveNoViolations(byte validXp) {
        Priority priority = Priority.builder()
                .name("HIGH")
                .xp(validXp)
                .build();

        Set<ConstraintViolation<Priority>> violations = validator.validate(priority);

        assertThat(violations).isEmpty();
    }
}