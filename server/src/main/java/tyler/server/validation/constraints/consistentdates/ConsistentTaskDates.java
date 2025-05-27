package tyler.server.validation.constraints.consistentdates;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConsistentTaskDatesValidator.class)
public @interface ConsistentTaskDates {
    String message() default "Task dates are inconsistent";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}