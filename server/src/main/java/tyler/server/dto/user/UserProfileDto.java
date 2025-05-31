package tyler.server.dto.user;

import java.time.LocalDate;
import java.util.Set;

public record UserProfileDto(
        String username,
        int currentXp,
        int dailyQuota,
        int currentStreak,
        byte daysOffPerWeek,
        Set<LocalDate> daysOff,
        LocalDate lastAchievedDate
) {}