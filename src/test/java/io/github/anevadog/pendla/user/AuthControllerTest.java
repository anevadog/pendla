package io.github.anevadog.pendla.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.anevadog.pendla.common.SecurityConfig;
import io.github.anevadog.pendla.user.dto.TokenResponse;
import io.github.anevadog.pendla.user.dto.UserResponse;

/**
 * Web layer tests: real controller, validation and GlobalExceptionHandler; the service is a mock.
 */
@WebMvcTest(AuthController.class)
// Public endpoints: the tests send no token, with the real security rules loaded
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=test-secret-with-at-least-32-characters")
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService service;

	@Test
	void register_withValidBody_returns201WithoutPassword() throws Exception {
		when(service.register(any())).thenReturn(new UserResponse(1L, "alex"));

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username": "alex", "password": "secret-password"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.username").value("alex"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void register_withShortPassword_returns400() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username": "alex", "password": "short"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.password").exists());
		verify(service, never()).register(any());
	}

	@Test
	void register_withInvalidUsername_returns400() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username": "a b", "password": "secret-password"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.username").exists());
		verify(service, never()).register(any());
	}

	@Test
	void register_withEmptyBody_returns400ForBothFields() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.username").exists())
				.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void register_withExistingUsername_returns409() throws Exception {
		when(service.register(any())).thenThrow(new UsernameAlreadyExistsException("alex"));

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username": "alex", "password": "secret-password"}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Username 'alex' is already taken"));
	}

	@Test
	void login_withValidCredentials_returns200WithToken() throws Exception {
		when(service.login(any()))
				.thenReturn(new TokenResponse("header.payload.signature", Instant.parse("2026-11-16T11:00:00Z")));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username": "alex", "password": "secret-password"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("header.payload.signature"))
				.andExpect(jsonPath("$.expiresAt").value("2026-11-16T11:00:00Z"));
	}

	@Test
	void login_withWrongCredentials_returns401() throws Exception {
		when(service.login(any())).thenThrow(new InvalidCredentialsException());

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username": "alex", "password": "wrong-password"}
						"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Invalid username or password"));
	}

	@Test
	void login_withBlankFields_returns400() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username": " ", "password": ""}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.username").exists())
				.andExpect(jsonPath("$.errors.password").exists());
		verify(service, never()).login(any());
	}

	@Test
	void register_withPasswordOver72Bytes_returns400() throws Exception {
		when(service.register(any())).thenThrow(new PasswordTooLongException());

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username": "alex", "password": "secret-password"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

}
