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
import tyler.server.exception.BusinessValidationException;
import tyler.server.repository.UserRepository;
import tyler.server.validation.constraints.currentweek.CurrentWeek;

import java.time.LocalDate;
import java.util.List;

@Service
@Validated
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        user.getDaysOff().add(dayOff);
        user.setDaysOffPerWeek(--daysOffPerWeek);
        userRepository.save(user);
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
