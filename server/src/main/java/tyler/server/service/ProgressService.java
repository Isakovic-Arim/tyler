package tyler.server.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tyler.server.entity.Task;
import tyler.server.entity.User;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class ProgressService {
    public void handleTaskCompletion(Task task) {
        User user = task.getUser();
        if (user == null) return;

        // Don't add XP on off days, but still need to check streak
        boolean isOffDay = user.getDaysOff().contains(LocalDate.now());
        if (!isOffDay) {
            user.setCurrentXp(user.getCurrentXp() + task.getRemainingXp());
        }

        if (isOffDay || user.getCurrentXp() >= user.getDailyXpQuota()) {
            LocalDate today = LocalDate.now();
            LocalDate lastAchieved = user.getLastAchievedDate();

            if (lastAchieved == null || !lastAchieved.equals(today)) {
                updateStreak(user, today, lastAchieved);

                // Only deduct XP quota on non-off days
                if (!isOffDay) {
                    user.setCurrentXp(user.getCurrentXp() - user.getDailyXpQuota());
                }
                user.setLastAchievedDate(today);
            }
        }
    }

    @Transactional
    public void relocateTasksForOffDays(User user) {
        LocalDate today = LocalDate.now();
        List<Task> tasksToRelocate = user.getTasks().stream()
                .filter(task -> !task.isDone() && task.getDueDate() != null &&
                        (user.getDaysOff().contains(task.getDueDate()) ||
                                user.getDaysOff().contains(today)))
                .sorted(Comparator.comparing(Task::getDeadline))
                .toList();

        for (Task task : tasksToRelocate) {
            LocalDate newDueDate = findNextAvailableDate(user, task.getDueDate());
            if (newDueDate != null && !newDueDate.isAfter(task.getDeadline())) {
                task.setDueDate(newDueDate);
            }
        }
    }

    private LocalDate findNextAvailableDate(User user, LocalDate currentDate) {
        LocalDate nextDate = currentDate.plusDays(1);
        while (user.getDaysOff().contains(nextDate)) {
            nextDate = nextDate.plusDays(1);
        }
        return nextDate;
    }

    private void updateStreak(User user, LocalDate today, LocalDate lastAchieved) {
        if (lastAchieved == null) {
            user.setCurrentStreak(1);
            return;
        }

        // Check if yesterday was the last achieved date
        if (lastAchieved.equals(today.minusDays(1))) {
            user.setCurrentStreak(user.getCurrentStreak() + 1);
            return;
        }

        // Check if all days between lastAchieved and today were off days
        boolean allDaysWereOffDays = true;
        LocalDate checkDate = lastAchieved.plusDays(1);

        while (checkDate.isBefore(today)) {
            if (!user.getDaysOff().contains(checkDate)) {
                allDaysWereOffDays = false;
                break;
            }
            checkDate = checkDate.plusDays(1);
        }

        if (allDaysWereOffDays) {
            user.setCurrentStreak(user.getCurrentStreak() + 1);
        } else {
            user.setCurrentStreak(1);
        }
    }
}