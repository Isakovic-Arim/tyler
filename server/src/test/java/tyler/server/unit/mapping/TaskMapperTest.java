package tyler.server.unit.mapping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tyler.server.entity.Task;
import tyler.server.dto.task.TaskRequestDTO;
import tyler.server.mapper.TaskMapperImpl;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class TaskMapperTest {
    @Spy
    @InjectMocks
    private TaskMapperImpl taskMapper;

    @Test
    public void requestDtoToTask_ShouldMapAllFields_WhenAllPropertiesProvided() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(7);

        TaskRequestDTO dto = new TaskRequestDTO(
            2L,
            "Test Task",
            "Task Description",
            today,
            tomorrow,
            1L
        );

        Task result = taskMapper.RequestDtoToTask(dto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Task");
        assertThat(result.getDescription()).isEqualTo("Task Description");
        assertThat(result.getDueDate()).isEqualTo(today);
        assertThat(result.getDeadline()).isEqualTo(tomorrow);
        assertThat(result.isDone()).isFalse();
        assertThat(result.getId()).isNull();
    }

    @Test
    public void requestDtoToTask_ShouldMapNullValues_WhenOptionalFieldsNotProvided() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        TaskRequestDTO dto = new TaskRequestDTO(
            null,
            "Minimal Task",
            null,
            null,
            tomorrow,
            1L
        );

        Task result = taskMapper.RequestDtoToTask(dto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Minimal Task");
        assertThat(result.getDescription()).isNull();
        assertThat(result.getDueDate()).isNull();
        assertThat(result.getDeadline()).isEqualTo(tomorrow);
        assertThat(result.getParent()).isNull();
        assertThat(result.isDone()).isFalse();
    }
}