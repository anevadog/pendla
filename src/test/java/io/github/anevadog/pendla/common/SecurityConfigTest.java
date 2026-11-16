package io.github.anevadog.pendla.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.anevadog.pendla.application.JobApplicationController;
import io.github.anevadog.pendla.application.JobApplicationService;

/**
 * The access rules, tested with real tokens against one protected controller.
 * Nothing is mocked in the security part: the filters, the decoder and the 401 are the real ones.
 */
@WebMvcTest(JobApplicationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=test-secret-with-at-least-32-characters")
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	// The same encoder the application uses, so its tokens are signed with the test secret
	@Autowired
	private JwtEncoder jwtEncoder;

	@MockitoBean
	private JobApplicationService service;

	@Test
	void protectedEndpoint_withoutToken_returns401ProblemDetail() throws Exception {
		mockMvc.perform(get("/api/applications"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.title").value("Unauthorized"))
				.andExpect(jsonPath("$.detail").exists());
		verify(service, never()).findAll(any());
	}

	@Test
	void protectedEndpoint_withValidToken_returns200() throws Exception {
		when(service.findAll(any())).thenReturn(List.of());
		String token = token(jwtEncoder, Instant.now().plusSeconds(600));

		mockMvc.perform(get("/api/applications").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void protectedEndpoint_withExpiredToken_returns401() throws Exception {
		// Expired ten minutes ago: more than the 60 seconds of clock difference the decoder tolerates
		String token = token(jwtEncoder, Instant.now().minusSeconds(600));

		mockMvc.perform(get("/api/applications").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void protectedEndpoint_withTokenSignedWithAnotherKey_returns401() throws Exception {
		// A well-formed token that is not expired, but signed by someone who does not know our secret
		var otherKey = new SecretKeySpec(
				"another-secret-with-at-least-32-characters".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		JwtEncoder otherEncoder = NimbusJwtEncoder.withSecretKey(otherKey).algorithm(MacAlgorithm.HS256).build();
		String token = token(otherEncoder, Instant.now().plusSeconds(600));

		mockMvc.perform(get("/api/applications").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpoint_withTextThatIsNotAToken_returns401() throws Exception {
		mockMvc.perform(get("/api/applications").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void writeEndpoint_withoutToken_returns401AndDoesNothing() throws Exception {
		// With CSRF protection on, this would be a 403. It is off because there are no cookies
		mockMvc.perform(delete("/api/applications/1"))
				.andExpect(status().isUnauthorized());
		verify(service, never()).delete(any());
	}

	private static String token(JwtEncoder encoder, Instant expiresAt) {
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject("alex")
				.issuedAt(expiresAt.minusSeconds(3600))
				.expiresAt(expiresAt)
				.build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

}
