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
import tyler.server.entity.User;
import tyler.server.repository.UserRepository;
import tyler.server.service.UserService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

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
}
