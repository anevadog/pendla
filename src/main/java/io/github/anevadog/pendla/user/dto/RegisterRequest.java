package io.github.anevadog.pendla.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/auth/register.
 */
public record RegisterRequest(

		@Schema(description = "3 to 30 letters, digits or underscores. Not case-sensitive", example = "alex")
		@NotNull
		@Pattern(regexp = "[A-Za-z0-9_]{3,30}", message = "must be 3 to 30 letters, digits or underscores")
		String username,

		@Schema(description = "8 to 72 characters", example = "correct-horse-battery")
		@NotBlank
		@Size(min = 8, max = 72)
		String password) {
}
