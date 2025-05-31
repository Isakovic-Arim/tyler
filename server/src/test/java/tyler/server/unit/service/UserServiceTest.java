package tyler.server.unit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import tyler.server.entity.User;
import tyler.server.repository.UserRepository;
import tyler.server.service.ProgressService;
import tyler.server.service.UserService;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProgressService progressService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .passwordHash("hashedPassword123")
                .currentXp(100)
                .dailyXpQuota(50)
                .currentStreak(3)
                .build();
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenUserExists() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userService.loadUserByUsername("testuser");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("hashedPassword123");
        assertThat(userDetails.getAuthorities())
                .hasSize(1)
                .first()
                .isInstanceOf(SimpleGrantedAuthority.class)
                .satisfies(authority ->
                        assertThat(authority.getAuthority()).isEqualTo("ROLE_USER")
                );

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void loadUserByUsername_ShouldHandleNullUsername() {
        assertThatThrownBy(() -> userService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findByUsername(null);
    }

    @Test
    void findByUsername_ShouldReturnUser_WhenUserExists() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        User result = userService.findByUsername("testuser");

        assertThat(result).isEqualTo(testUser);
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void findByUsername_ShouldThrowException_WhenUserDoesNotExist() {
        when(userRepository.findByUsername("wrong")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUsername("wrong"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findByUsername("wrong");
    }

    @Test
    void getUserFromJwt_ShouldThrowException_WhenSubjectIsNull() {
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getSubject()).thenReturn(null);

        assertThatThrownBy(() -> userService.getUserFromJwt(mockJwt))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findByUsername(null);
    }

    @Test
    void setDayOff_ShouldAddDayOff() {
        String username = "testuser";
        LocalDate dayOff = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        userService.setDayOff(username, dayOff);

        assertThat(testUser.getDaysOff()).contains(dayOff);
    }

    @Test
    void removeDayOff_ShouldRemoveDayOff() {
        String username = "testuser";
        LocalDate dayOff = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        testUser.getDaysOff().add(dayOff);
        testUser.setDaysOffPerWeek((byte) 1);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        userService.removeDayOff(username, dayOff);

        assertThat(testUser.getDaysOff()).doesNotContain(dayOff);
        assertThat(testUser.getDaysOffPerWeek()).isEqualTo((byte) 2);
        verify(userRepository).save(testUser);
    }

    @Test
    void resetDaysOffPerWeek_ShouldResetDaysOffPerWeek() {
        User user = new User();
        user.setDaysOffPerWeek((byte) 0);
        user.getDaysOff().add(LocalDate.now());

        when(userRepository.findAll()).thenReturn(List.of(user));

        userService.resetDaysOffPerWeek();

        assertThat(user.getDaysOffPerWeek()).isEqualTo((byte) 2);
        assertThat(user.getDaysOff()).isEmpty();
        verify(userRepository).save(user);
    }
}
