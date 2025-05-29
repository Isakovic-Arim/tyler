package tyler.server.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tyler.server.entity.Task;
import tyler.server.entity.User;
import tyler.server.repository.TaskRepository;
import tyler.server.repository.UserRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class ProgressService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public ProgressService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

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
                relocateSubtasks(task, user);
            }
        }
    }

    private void relocateSubtasks(Task parentTask, User user) {
        for (Task subtask : parentTask.getSubtasks()) {
            if (!subtask.isDone() && subtask.getDueDate() != null) {
                LocalDate newDueDate = findNextAvailableDate(user, subtask.getDueDate());
                if (newDueDate != null && !newDueDate.isAfter(subtask.getDeadline()) &&
                        !newDueDate.isAfter(parentTask.getDueDate())) {
                    subtask.setDueDate(newDueDate);
                }
            }
        }
    }

    private LocalDate findNextAvailableDate(User user, LocalDate currentDate) {
        LocalDate nextDate = currentDate;
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

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void checkDailyStreaks() {
        // First, check and update streaks for users who have enough XP
        userRepository.findUsersWithEnoughXpForDailyQuota().forEach(user -> {
            LocalDate today = LocalDate.now();
            LocalDate lastAchieved = user.getLastAchievedDate();

            if (lastAchieved == null || !lastAchieved.equals(today)) {
                updateStreak(user, today, lastAchieved);
                user.setLastAchievedDate(today);
                user.setCurrentXp(user.getCurrentXp() - user.getDailyXpQuota());
            }
        });

        // Then, reset streaks for users who haven't met their quota
        userRepository.findUsersWhoAreNotOffAndMissedDailyQuotaToday().forEach(user -> {
            user.setCurrentStreak(0);
        });

        userRepository.saveAll(userRepository.findUsersWithEnoughXpForDailyQuota());
        userRepository.saveAll(userRepository.findUsersWhoAreNotOffAndMissedDailyQuotaToday());
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void penalizeForOverDeadlineTasks() {
        taskRepository.findAllTasksOverDeadline()
                .forEach(task -> {
                    User user = task.getUser();
                    user.setCurrentXp(user.getCurrentXp() - 1);
                });
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void penalizeForOverDueDateTasks() {
        taskRepository.findAllTasksOverDueDate()
                .forEach(task -> {
                    task.setRemainingXp((byte) Math.min((task.getRemainingXp() - 1), 0));
                });
    }
}