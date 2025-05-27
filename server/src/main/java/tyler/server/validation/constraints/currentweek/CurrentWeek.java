package tyler.server.validation.constraints.currentweek;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CurrentWeekValidator.class)
public @interface CurrentWeek {
    String message() default "The days off must be taken within the current week (from Monday to Sunday)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
