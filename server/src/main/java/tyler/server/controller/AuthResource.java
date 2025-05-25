package tyler.server.controller;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tyler.server.dto.auth.AuthRequest;
import tyler.server.service.AuthService;

import java.util.UUID;

import static org.springframework.http.HttpHeaders.SET_COOKIE;

@RestController
@RequestMapping("/auth")
public class AuthResource {
    private final AuthService authService;

    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody AuthRequest request) {
        authService.register(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody AuthRequest request) {
        var response = authService.login(request);

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", response.accessToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(900)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", response.refreshToken().toString())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/auth")
                .maxAge(2592000)
                .build();

        return ResponseEntity.ok()
                .header(SET_COOKIE, accessCookie.toString())
                .header(SET_COOKIE, refreshCookie.toString())
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(@CookieValue("refreshToken") UUID refreshToken) {
        String token = authService.refreshAccessToken(refreshToken);

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(900)
                .build();

        return ResponseEntity.ok()
                .header(SET_COOKIE, accessCookie.toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue("refreshToken") UUID refreshToken) {
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();

        authService.revokeRefreshToken(refreshToken);
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/auth")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(SET_COOKIE, accessCookie.toString())
                .header(SET_COOKIE, refreshCookie.toString())
                .build();
    }
}
