package io.github.anevadog.pendla.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import io.github.anevadog.pendla.common.SecurityConfig;
import io.github.anevadog.pendla.user.dto.TokenResponse;

/**
 * Unit tests with the real encoder and decoder, built by hand from SecurityConfig. No Spring context.
 */
class TokenServiceTest {

	private final SecurityConfig config = new SecurityConfig();
	private final SecretKey key = config.jwtSecretKey("test-secret-with-at-least-32-characters");
	private final TokenService service = new TokenService(config.jwtEncoder(key), Duration.ofHours(1));

	@Test
	void createToken_withUsername_returnsTokenTheDecoderAccepts() {
		Instant before = Instant.now();

		TokenResponse response = service.createToken("alex");

		// decode() checks the signature and the expiry, and throws if either is wrong
		Jwt jwt = config.jwtDecoder(key).decode(response.token());
		assertThat(jwt.getSubject()).isEqualTo("alex");
		assertThat(response.expiresAt()).isBetween(before.plus(59, ChronoUnit.MINUTES), before.plus(61, ChronoUnit.MINUTES));
		// A JWT stores times in whole seconds
		assertThat(jwt.getExpiresAt()).isEqualTo(response.expiresAt().truncatedTo(ChronoUnit.SECONDS));
	}

	@Test
	void jwtSecretKey_withShortSecret_throwsIllegalState() {
		assertThatThrownBy(() -> config.jwtSecretKey("too-short"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("at least 32");
	}

}
