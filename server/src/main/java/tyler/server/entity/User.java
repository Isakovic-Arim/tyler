package tyler.server.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import tyler.server.common.Role;
import tyler.server.validation.constraints.currentweek.CurrentWeek;

import java.time.LocalDate;
import java.util.*;

@Entity
@Table(
        name = "app_user",
        indexes = @Index(name = "user_username_idx", columnList = "username", unique = true)
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "username", unique = true, nullable = false)
    @NotNull(message = "Username cannot be null")
    private String username;

    @Column(name = "password_hash", nullable = false)
    @NotNull(message = "Password cannot be null")
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<Role> roles = Set.of(Role.ROLE_USER);

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Task> tasks = new ArrayList<>();

    @Column(name = "current_xp", nullable = false)
    @Min(value = 0, message = "Current XP cannot be negative")
    private int currentXp;

    @Column(name = "daily_xp_quota", nullable = false)
    @Min(value = 0, message = "Daily XP quota cannot be negative")
    @Builder.Default
    private int dailyXpQuota = 5;

    @Column(name = "current_streak", nullable = false)
    @Min(value = 0, message = "Current streak cannot be negative")
    private int currentStreak;

    @Column(name = "last_achieved_date")
    private LocalDate lastAchievedDate;

    @Column(name = "days_off_per_week", nullable = false)
    @Max(value = 2, message = "Days off per week cannot exceed 2")
    @Builder.Default
    private byte daysOffPerWeek = 2;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "days_off", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "day_of_week")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<@CurrentWeek LocalDate> daysOff = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    public void addTask(Task task) {
        task.setUser(this);
        tasks.add(task);
    }

    public void removeTask(Task task) {
        task.setUser(null);
        tasks.remove(task);
    }

    public void addRefreshToken(RefreshToken refreshToken) {
        refreshToken.setUser(this);
        refreshTokens.add(refreshToken);
    }

    public void removeRefreshToken(RefreshToken refreshToken) {
        refreshToken.setUser(null);
        refreshTokens.remove(refreshToken);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User )) return false;
        return id != null && id.equals(((User) o).getId());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> (GrantedAuthority) role::name)
                .toList();
    }

    @Override
    public String getPassword() {
        return null;
    }
}
