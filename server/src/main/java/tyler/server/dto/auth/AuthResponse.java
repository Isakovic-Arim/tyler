package tyler.server.dto.auth;

import java.util.UUID;

public record AuthResponse(String accessToken, UUID refreshToken) { }