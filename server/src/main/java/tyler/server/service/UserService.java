package tyler.server.service;

import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import tyler.server.entity.User;
import tyler.server.repository.UserRepository;
import tyler.server.validation.constraints.currentweek.CurrentWeek;

import java.time.LocalDate;
import java.util.List;

@Service
@Validated
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final ProgressService progressService;

    public UserService(UserRepository userRepository, ProgressService progressService) {
        this.userRepository = userRepository;
        this.progressService = progressService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public User getUserFromJwt(Jwt jwt) {
        String username = jwt.getSubject();
        return findByUsername(username);
    }

    @Transactional
    public void setDayOff(String username, @CurrentWeek LocalDate dayOff) {
        User user = findByUsername(username);
        byte daysOffPerWeek = user.getDaysOffPerWeek();
        if (daysOffPerWeek < 1) {
            throw new IllegalStateException("No days off available for this week");
        }
        if (user.getDaysOff().contains(dayOff)) {
            throw new IllegalStateException("Day off already set for this date");
        }
        if (user.getTasks().stream().anyMatch(task -> task.getDeadline().isEqual(dayOff))) {
            throw new IllegalStateException("Cannot take a day off if you have a deadline on that day");
        }
        user.getDaysOff().add(dayOff);
        user.setDaysOffPerWeek(--daysOffPerWeek);
        progressService.relocateTasksForOffDays(user);
    }

    public void removeDayOff(String username, @CurrentWeek LocalDate dayOff) {
        User user = findByUsername(username);
        if (!user.getDaysOff().remove(dayOff)) {
            throw new IllegalStateException("Day off not found for this date");
        }
        if (dayOff.isEqual(LocalDate.now())) {
            throw new IllegalStateException("Cannot remove today's day off");
        }
        user.setDaysOffPerWeek((byte) (user.getDaysOffPerWeek() + 1));
        userRepository.save(user);
        progressService.relocateTasksForOffDays(user);
    }

    @Scheduled(cron = "59 59 23 * * ?")
    public void revokeDayOff() {
        userRepository.findUsersWithDayOffToday()
                .forEach(user -> {
                    user.getDaysOff().removeIf(dayOff -> dayOff.isEqual(LocalDate.now()));
                    userRepository.save(user);
                });
    }

    @Scheduled(cron = "59 59 23 * * 0")
    public void resetDaysOffPerWeek() {
        userRepository.findAll().forEach(user -> {
            user.setDaysOffPerWeek((byte) 2);
            user.getDaysOff().clear();
            userRepository.save(user);
        });
    }
}
