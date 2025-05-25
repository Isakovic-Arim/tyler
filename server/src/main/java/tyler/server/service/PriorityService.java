package tyler.server.service;

import org.springframework.stereotype.Service;
import tyler.server.dto.priority.PriorityResponseDto;
import tyler.server.mapper.PriorityMapper;
import tyler.server.repository.PriorityRepository;

import java.util.List;

@Service
public class PriorityService {
    private final PriorityRepository priorityRepository;
    private final PriorityMapper priorityMapper;


    public PriorityService(PriorityRepository priorityRepository, PriorityMapper priorityMapper) {
        this.priorityRepository = priorityRepository;
        this.priorityMapper = priorityMapper;
    }

    public List<PriorityResponseDto> findAllPriorities() {
        return priorityRepository.findAllPriorities().stream()
                .map(priorityMapper::toResponseDto).toList();
    }
}
