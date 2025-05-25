package tyler.server.service;

import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import tyler.server.dto.auth.AuthRequest;
import tyler.server.dto.auth.AuthResponse;
import tyler.server.entity.RefreshToken;
import tyler.server.entity.User;
import tyler.server.repository.RefreshTokenRepository;
import tyler.server.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtEncoder jwtEncoder;

    public AuthService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authManager,
            JwtEncoder jwtEncoder
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authManager = authManager;
        this.jwtEncoder = jwtEncoder;
    }

    @Transactional
    public void register(AuthRequest request) {
        if (userRepository.existsByUsername((request.username()))) {
            throw new ValidationException("Username already exists");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        Instant now = Instant.now();
        String accessToken = jwtEncoder.encode(
                JwtEncoderParameters.from(JwtClaimsSet.builder()
                        .subject(request.username())
                        .issuedAt(now)
                        .expiresAt(now.plus(15, ChronoUnit.MINUTES))
                        .build()
                )
        ).getTokenValue();

        RefreshToken refreshToken = RefreshToken.builder()
                .expiresAt(now.plus(30, ChronoUnit.DAYS))
                .build();

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.addRefreshToken(refreshToken);
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, refreshToken.getId());
    }

    public String refreshAccessToken(UUID refreshToken) {
        final RefreshToken token = refreshTokenRepository
                .findByIdAndExpiresAtAfter(refreshToken, Instant.now())
                .orElseThrow(() -> new ValidationException("Invalid or expired refresh token"));

        return jwtEncoder.encode(
                JwtEncoderParameters.from(JwtClaimsSet.builder()
                        .subject(token.getUser().getUsername())
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                        .build()
                )
        ).getTokenValue();
    }

    public void revokeRefreshToken(UUID token) {
        refreshTokenRepository.deleteById(token);
    }
}
