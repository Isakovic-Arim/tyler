package tyler.server.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tyler.server.entity.Task;
import tyler.server.entity.User;

import java.time.LocalDate;

@Service
public class ProgressService {
    @Transactional
    public void handleTaskCompletion(Task task) {
        User user = task.getUser();
        if (user == null) return;

        user.setCurrentXp(user.getCurrentXp() + task.getPriority().getXp());

        if (user.getCurrentXp() >= user.getDailyXpQuota()) {
            LocalDate today = LocalDate.now();
            LocalDate lastAchieved = user.getLastAchievedDate();

            if (lastAchieved == null || !lastAchieved.equals(today)) {
                if (lastAchieved != null && lastAchieved.equals(today.minusDays(1))) {
                    user.setCurrentStreak(user.getCurrentStreak() + 1);
                } else {
                    user.setCurrentStreak(1);
                }
                user.setCurrentXp(user.getCurrentXp() - user.getDailyXpQuota());
                user.setLastAchievedDate(today);
            }
        }
    }
}