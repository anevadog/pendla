package io.github.anevadog.pendla.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.anevadog.pendla.application.dto.CreateJobApplicationRequest;
import io.github.anevadog.pendla.application.dto.JobApplicationResponse;

/**
 * Business logic for job applications. Receives and returns DTOs; entities stay inside.
 */
@Service
public class JobApplicationService {

	private final JobApplicationRepository repository;

	public JobApplicationService(JobApplicationRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public JobApplicationResponse create(CreateJobApplicationRequest request) {
		String jobtechId = blankToNull(request.jobtechId());

		if (jobtechId != null && repository.existsByJobtechId(jobtechId)) {
			throw new DuplicateJobApplicationException(jobtechId);
		}

		JobApplication application = new JobApplication(
				jobtechId,
				request.title().trim(),
				request.employer().trim(),
				blankToNull(request.city()),
				blankToNull(request.url()));

		return JobApplicationResponse.from(repository.save(application));
	}

	/** All applications, newest first. readOnly tells Hibernate it does not need to track changes. */
	@Transactional(readOnly = true)
	public List<JobApplicationResponse> findAll() {
		return repository.findAllByOrderByCreatedAtDesc().stream()
				.map(JobApplicationResponse::from)
				.toList();
	}

	/** Optional fields: store "" or "   " as null, so empty values mean the same thing everywhere. */
	private static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.trim();
	}

}
