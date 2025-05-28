package tyler.server.entity;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import tyler.server.validation.constraints.consistentdates.ConsistentTaskDates;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static tyler.server.Constants.*;

@Entity
@Table(name = "task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ConsistentTaskDates
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Task parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Task> subtasks = new ArrayList<>();

    @NotNull(message = "Priority is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @Valid
    private Priority priority;

    @NotNull(message = "Remaining xp must be tracked")
    @Min(value = 0, message = "Remaining xp cannot reach below zero")
    private byte remainingXp;

    @NotBlank(message = "Task name cannot be blank")
    @Size(min = 3, max = MAX_TASK_NAME_LENGTH, message = "Task name must be between 3 and " + MAX_TASK_NAME_LENGTH + " characters")
    @Column(name = "name", nullable = false, length = MAX_TASK_NAME_LENGTH)
    private String name;

    @Size(max = MAX_TASK_DESCRIPTION_LENGTH, message = "Description cannot exceed " + MAX_TASK_DESCRIPTION_LENGTH + " characters")
    @Column(name = "description", length = MAX_TASK_DESCRIPTION_LENGTH)
    private String description;

    @FutureOrPresent(message = "Due date must be in the future or present")
    @Column(name = "due_date")
    private LocalDate dueDate;

    @NotNull(message = "Deadline is required")
    @FutureOrPresent(message = "Deadline must be in the future or present")
    @Column(name = "deadline", nullable = false)
    private LocalDate deadline;

    @Column(name = "done", nullable = false)
    private boolean done;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task )) return false;
        return id != null && id.equals(((Task) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public void addSubtask(Task subtask) {
        subtasks.add(subtask);
        subtask.setParent(this);
    }

    public void removeSubtask(Task subtask) {
        subtasks.remove(subtask);
        subtask.setParent(null);
    }
}