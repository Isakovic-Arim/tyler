package tyler.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tyler.server.dto.priority.PriorityResponseDto;
import tyler.server.service.PriorityService;

import java.util.List;

@RestController
@RequestMapping("priorities")
public class PriorityResource {
    private final PriorityService priorityService;

    public PriorityResource(PriorityService priorityService) {
        this.priorityService = priorityService;
    }

    @GetMapping
    public ResponseEntity<List<PriorityResponseDto>> getPriorities() {
        return ResponseEntity.ok().body(priorityService.findAllPriorities());
    }
}
