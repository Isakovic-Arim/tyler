package tyler.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tyler.server.dto.user.UserProfileDto;
import tyler.server.entity.User;
import tyler.server.mapper.UserMapper;
import tyler.server.service.UserService;

import java.security.Principal;

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
}
