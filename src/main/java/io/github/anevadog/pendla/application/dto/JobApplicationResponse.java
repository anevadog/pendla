package io.github.anevadog.pendla.application.dto;

import java.time.Instant;

import io.github.anevadog.pendla.application.ApplicationStatus;
import io.github.anevadog.pendla.application.JobApplication;

/**
 * What the API returns for a job application.
 */
public record JobApplicationResponse(
		Long id,
		String jobtechId,
		String title,
		String employer,
		String city,
		String url,
		ApplicationStatus status,
		Instant createdAt) {

	public static JobApplicationResponse from(JobApplication application) {
		return new JobApplicationResponse(
				application.getId(),
				application.getJobtechId(),
				application.getTitle(),
				application.getEmployer(),
				application.getCity(),
				application.getUrl(),
				application.getStatus(),
				application.getCreatedAt());
	}

}
