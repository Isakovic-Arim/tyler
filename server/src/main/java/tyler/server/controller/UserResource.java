package tyler.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tyler.server.dto.user.UserProfileDto;
import tyler.server.entity.User;
import tyler.server.mapper.UserMapper;
import tyler.server.service.UserService;
import tyler.server.validation.constraints.currentweek.CurrentWeek;

import java.security.Principal;
import java.time.LocalDate;

@RestController
@RequestMapping("/users")
public class UserResource {
    private final UserService userService;
    private final UserMapper userMapper;

    public UserResource(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getUserByUsername(Principal principal) {
        User user = userService.findByUsername(principal.getName());
        return ResponseEntity.ok(userMapper.toUserProfileDto(user));
    }

    @PostMapping("/me/day-off")
    public ResponseEntity<Void> setDayOff(Principal principal, @RequestBody @CurrentWeek LocalDate dayOff) {
        userService.setDayOff(principal.getName(), dayOff);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/day-off")
    public ResponseEntity<Void> removeDayOff(Principal principal, @RequestParam LocalDate date) {
        userService.removeDayOff(principal.getName(), date);
        return ResponseEntity.noContent().build();
    }
}
