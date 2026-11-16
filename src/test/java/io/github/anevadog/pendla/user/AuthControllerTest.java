package io.github.anevadog.pendla.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.anevadog.pendla.user.dto.UserResponse;

/**
 * Web layer tests: real controller, validation and GlobalExceptionHandler; the service is a mock.
 */
@WebMvcTest(AuthController.class)
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
