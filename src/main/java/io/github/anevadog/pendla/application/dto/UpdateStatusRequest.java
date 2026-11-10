package io.github.anevadog.pendla.application.dto;

import io.github.anevadog.pendla.application.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Body of PATCH /api/applications/{id}/status.
 * A value that is not an ApplicationStatus (e.g. "FOO") fails while reading the JSON and returns 400
 * with the list of valid values (see GlobalExceptionHandler).
 */
public record UpdateStatusRequest(

		@Schema(description = "New status. Must be an allowed transition from the current one", example = "SENT")
		@NotNull
		ApplicationStatus status) {
}
