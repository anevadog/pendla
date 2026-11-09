package io.github.anevadog.pendla;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web layer test: only loads PingController, no database needed.
 */
@WebMvcTest(PingController.class)
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
