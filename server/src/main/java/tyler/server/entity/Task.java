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
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank(message = "Task name cannot be blank")
    @Size(min = 3, max = MAX_NAME_LENGTH, message = "Task name must be between 3 and 100 characters")
    private String name;

    @Size(max = MAX_DESCRIPTION_LENGTH, message = "Description cannot exceed 500 characters")
    private String description;

    @FutureOrPresent(message = "Due date must be in the future or present")
    private LocalDate dueDate;

    @NotNull(message = "Deadline is required")
    @FutureOrPresent(message = "Deadline must be in the future or present")
    private LocalDate deadline;

    @Min(value = 1, message = "XP must be at least 1")
    @Max(value = MAX_XP, message = "XP cannot exceed 100")
    private byte xp;

    private boolean done;
}