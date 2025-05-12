package tyler.server.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import static tyler.server.Constants.MAX_PRIORITY_NAME_LENGTH;
import static tyler.server.Constants.MAX_PRIORITY_XP;

@Entity
@Table(name = "priority")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Priority {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @NotBlank(message = "Priority name cannot be blank")
    @Size(min = 3, max = MAX_PRIORITY_NAME_LENGTH, message = "Priority name must be between 3 and " + MAX_PRIORITY_NAME_LENGTH + " characters")
    @Column(name = "name", nullable = false, length = MAX_PRIORITY_NAME_LENGTH)
    private String name;

    @Min(value = 1, message = "XP must be at least 1")
    @Max(value = MAX_PRIORITY_XP, message = "XP cannot exceed " + MAX_PRIORITY_XP)
    @Column(name = "xp", nullable = false)
    private byte xp;
}
