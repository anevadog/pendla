package io.github.anevadog.pendla.application.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Body of PUT /api/applications/{id}/details. Replaces both fields:
 * a missing or null field clears it, so {} removes the notes and the interview date.
 */
public record UpdateDetailsRequest(

		@Schema(description = "Free-text notes. Do not include third-party contact data "
				+ "(recruiter names, emails or phone numbers)",
				example = "Technical interview with the backend team. Prepare Spring and SQL questions.")
		@Size(max = 2000)
		String notes,

		@Schema(description = "Interview date and time, ISO 8601 with an offset or Z. Returned in UTC",
				example = "2026-11-20T10:00:00+01:00")
		Instant interviewAt) {
}
