package io.github.anevadog.pendla.user;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import io.github.anevadog.pendla.user.dto.TokenResponse;

/**
 * Creates the JWT a user receives at login.
 */
@Service
public class TokenService {

	private final JwtEncoder jwtEncoder;
	private final Duration expiration;

	public TokenService(JwtEncoder jwtEncoder, @Value("${jwt.expiration}") Duration expiration) {
		this.jwtEncoder = jwtEncoder;
		this.expiration = expiration;
	}

	/**
	 * The token says who the user is (subject) and until when it is valid. It is signed, not encrypted:
	 * anyone can read it, but nobody can change it without the secret key. So it never carries secrets.
	 */
	public TokenResponse createToken(String username) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(expiration);

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(username)
				.issuedAt(now)
				.expiresAt(expiresAt)
				.build();

		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new TokenResponse(token, expiresAt);
	}

}
