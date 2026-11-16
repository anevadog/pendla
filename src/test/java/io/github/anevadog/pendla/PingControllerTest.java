package io.github.anevadog.pendla;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import io.github.anevadog.pendla.common.SecurityConfig;

/**
 * Web layer test: only loads PingController, no database needed.
 */
@WebMvcTest(PingController.class)
// Public endpoints: the tests send no token, with the real security rules loaded
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=test-secret-with-at-least-32-characters")
class PingControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void ping_returnsPong() throws Exception {
		mockMvc.perform(get("/api/ping"))
				.andExpect(status().isOk())
				.andExpect(content().string("pong"));
	}

}
