package ch.hslu.blebooking.backend.auth.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.hslu.blebooking.backend.auth.internal.RefreshToken;
import ch.hslu.blebooking.backend.auth.internal.RefreshTokenRepository;

@Service
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Creates a new refresh token for the user.
     * Returns the raw (unhashed) token to be sent to the client.
     */
    public String createRefreshToken(Long userId) {
        // 1. Generate a secure, random token
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // 2. Hash the token for secure storage in the DB
        String tokenHash = hashToken(rawToken);

        // 3. Define validity period (e.g., 30 days for long-lived sessions)
        Instant expiresAt = Instant.now().plus(30, ChronoUnit.DAYS);

        // 4. Save in the database
        RefreshToken refreshToken = new RefreshToken(userId, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    /**
     * Validates a refresh token against the raw token provided by the client.
     */
    public Optional<Long> validateAndGetUserId(String rawToken) {
        String tokenHash = hashToken(rawToken);
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken refreshToken = tokenOpt.get();

        // Check if the token is expired or revoked
        if (refreshToken.isExpired() || refreshToken.isRevoked()) {
            return Optional.empty();
        }

        return Optional.of(refreshToken.getUserId());
    }

    /**
     * Revokes a refresh token (e.g., upon sign-out).
     */
    public void revokeToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);
        });
    }

    /**
     * Helper method for creating a SHA-256 hash of the token.
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not hash token", e);
        }
    }
}