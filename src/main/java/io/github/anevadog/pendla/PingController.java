package io.github.anevadog.pendla;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal endpoint to check that the API is up and responding.
 */
@RestController
public class PingController {

	@GetMapping("/api/ping")
	public String ping() {
		return "pong";
	}

}
