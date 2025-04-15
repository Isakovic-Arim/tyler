package tyler.server.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import static tyler.server.Constants.*;

@Data
@Entity
@Table(name = "task")
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @NotBlank(message = "Task name cannot be blank")
    @Size(min = 3, max = MAX_NAME_LENGTH, message = "Task name must be between 3 and " + MAX_NAME_LENGTH + " characters")
    @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Size(max = MAX_DESCRIPTION_LENGTH, message = "Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters")
    @Column(name = "description", length = MAX_DESCRIPTION_LENGTH)
    private String description;

    @FutureOrPresent(message = "Due date must be in the future or present")
    @Column(name = "due_date")
    private LocalDate dueDate;

    @NotNull(message = "Deadline is required")
    @FutureOrPresent(message = "Deadline must be in the future or present")
    @Column(name = "deadline", nullable = false)
    private LocalDate deadline;

    @Min(value = 1, message = "XP must be at least 1")
    @Max(value = MAX_XP, message = "XP cannot exceed " + MAX_XP)
    @Column(name = "xp", nullable = false)
    private byte xp;

    @Column(name = "done", nullable = false)
    private boolean done;
}