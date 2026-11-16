package io.github.anevadog.pendla.user.dto;

import java.time.Instant;

/**
 * Answer of POST /api/auth/login. The token goes in the header "Authorization: Bearer <token>".
 */
public record TokenResponse(String token, Instant expiresAt) {
}
