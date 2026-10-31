package main.java.ch.hslu.blebooking.backend.auth;

import ch.hslu.blebooking.backend.auth.internal.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RefreshTokenService refreshTokenService;

    public AuthController(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Refreshes the access token using a valid refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body("Refresh token is required");
        }

        var userIdOpt = refreshTokenService.validateAndGetUserId(refreshToken);
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid, expired, or revoked refresh token");
        }

        // Hier wird im weiteren Verlauf ein neues Access-Token generiert und
        // zurückgegeben.
        // Für den Moment bestätigen wir die Validität und geben die User-ID zurück.
        return ResponseEntity.ok(Map.of(
                "message", "Token successfully refreshed",
                "userId", userIdOpt.get()));
    }

    /**
     * Revokes the refresh token during logout.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeToken(refreshToken);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}