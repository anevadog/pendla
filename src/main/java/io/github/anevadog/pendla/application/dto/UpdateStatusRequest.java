package io.github.anevadog.pendla.application.dto;

import io.github.anevadog.pendla.application.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Body of PATCH /api/applications/{id}/status.
 * A value that is not an ApplicationStatus (e.g. "FOO") fails while reading the JSON and returns 400.
 */
public record UpdateStatusRequest(

		@NotNull
		ApplicationStatus status) {
}
