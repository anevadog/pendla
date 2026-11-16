package io.github.anevadog.pendla.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Body of POST /api/auth/login. Only checks that both fields are present:
 * the format rules belong to the registration, and a wrong value is simply a failed login.
 */
public record LoginRequest(

		@Schema(example = "alex")
		@NotBlank
		String username,

		@Schema(example = "correct-horse-battery")
		@NotBlank
		String password) {
}
