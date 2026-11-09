package io.github.anevadog.pendla;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Minimal endpoint to check that the API is up and responding.
 */
@RestController
@Tag(name = "Health")
public class PingController {

	@Operation(summary = "Check that the API is up", description = "Returns pong")
	@GetMapping("/api/ping")
	public String ping() {
		return "pong";
	}

}
