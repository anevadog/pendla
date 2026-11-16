package io.github.anevadog.pendla;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Minimal endpoint to check that the API is up and responding.
 */
@RestController
@Tag(name = "Health")
// Empty on purpose: this endpoint is public, so Swagger shows it without the lock
@SecurityRequirements
public class PingController {

	@Operation(summary = "Check that the API is up", description = "Returns pong")
	@GetMapping("/api/ping")
	public String ping() {
		return "pong";
	}

}
