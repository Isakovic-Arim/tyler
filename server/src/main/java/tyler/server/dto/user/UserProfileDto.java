package tyler.server.dto.user;

import java.time.LocalDate;
import java.util.Set;

public record UserProfileDto(
        String username,
        int currentXp,
        int dailyQuota,
        int currentStreak,
        Set<LocalDate> daysOff
) {}