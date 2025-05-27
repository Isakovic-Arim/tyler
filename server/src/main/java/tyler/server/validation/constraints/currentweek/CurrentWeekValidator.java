package tyler.server.validation.constraints.currentweek;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.*;
import java.time.temporal.TemporalAdjusters;

public class CurrentWeekValidator implements ConstraintValidator<CurrentWeek, LocalDate> {
    @Override
    public void initialize(CurrentWeek constraintAnnotation) {

    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalDate now = LocalDate.now();
        LocalDate endOfWeek = now.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));

        return !value.isBefore(now)
                && !value.isAfter(endOfWeek);
    }
}
