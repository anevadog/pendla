package io.github.anevadog.pendla.common;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Who can call what, and how tokens and passwords are handled.
 *
 * Authentication is stateless: the server keeps no session. Every request to a protected
 * endpoint must carry a JWT in the header "Authorization: Bearer <token>", and Spring Security
 * checks its signature and expiry before the request reaches a controller.
 */
@Configuration
public class SecurityConfig {

	// HS256 needs a key of at least 256 bits
	private static final int MIN_SECRET_BYTES = 32;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationEntryPoint authenticationEntryPoint)
			throws Exception {
		http
				// CSRF attacks rely on cookies the browser sends by itself. There are no cookies here:
				// the token travels in a header that only our client adds
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/ping", "/api/auth/**").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.anyRequest().authenticated())
				// Read the JWT from the Authorization header and validate it with the JwtDecoder below
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(Customizer.withDefaults())
						.authenticationEntryPoint(authenticationEntryPoint));
		return http.build();
	}

	/**
	 * Called when a request has no token or an invalid one. This happens in the security filters,
	 * before any controller, so GlobalExceptionHandler would never see it. Passing the exception to
	 * Spring MVC's resolver sends it there, and the 401 is built in the same place as every other error.
	 */
	@Bean
	public AuthenticationEntryPoint authenticationEntryPoint(
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
		return (request, response, exception) -> resolver.resolveException(request, response, null, exception);
	}

	/** The key that signs and verifies tokens. Whoever knows it can create valid tokens. */
	@Bean
	public SecretKey jwtSecretKey(@Value("${jwt.secret}") String secret) {
		byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_BYTES + " characters long");
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	/** Creates signed tokens (used at login). */
	@Bean
	public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return NimbusJwtEncoder.withSecretKey(jwtSecretKey).algorithm(MacAlgorithm.HS256).build();
	}

	/** Checks the signature and the expiry of the token of each request. */
	@Bean
	public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
		return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
	}

	/**
	 * BCrypt hashes passwords with a random salt and is slow on purpose,
	 * so a stolen table of hashes is expensive to attack.
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
