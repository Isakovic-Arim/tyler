package tyler.server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Task> tasks = new ArrayList<>();

    @Column(name = "current_xp", nullable = false)
    private int currentXp;

    @Column(name = "daily_xp_quota", nullable = false)
    private int dailyXpQuota;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "last_achieved_date")
    private LocalDate lastAchievedDate;

    @Column(name = "days_off_per_week", nullable = false)
    private byte daysOffPerWeek;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "days_off", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "day_of_week")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<DayOfWeek> offDays = Set.of();

    public void addTask(Task task) {
        tasks.add(task);
        task.setUser(this);
    }

    public void removeTask(Task task) {
        tasks.remove(task);
        task.setUser(null);
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
}
