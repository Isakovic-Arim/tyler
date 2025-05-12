package tyler.server.mapper;

import jakarta.validation.ConstraintViolationException;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import tyler.server.entity.Priority;
import tyler.server.repository.PriorityRepository;

@Mapper(componentModel = "spring")
public abstract class PriorityMapper {
    protected PriorityRepository priorityRepository;

    @Autowired
    protected void setPriorityRepository(PriorityRepository priorityRepository) {
        this.priorityRepository = priorityRepository;
    }

    @Named("idToPriority")
    public Priority idToPriority(Long id) {
        return priorityRepository.findById(id).orElseThrow(
                () -> new ConstraintViolationException("Priority with ID " + id + " does not exist", null)
        );
    }
}