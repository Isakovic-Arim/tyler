package tyler.server.dto.auth;

public record AuthRequest(
        String username,
        String password
) { }