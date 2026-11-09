package io.github.anevadog.pendla.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.anevadog.pendla.application.dto.CreateJobApplicationRequest;
import io.github.anevadog.pendla.application.dto.JobApplicationResponse;
import jakarta.validation.Valid;

/**
 * HTTP layer for job applications. No logic and no try/catch: errors go to GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

	private final JobApplicationService service;

	public JobApplicationController(JobApplicationService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public JobApplicationResponse create(@Valid @RequestBody CreateJobApplicationRequest request) {
		return service.create(request);
	}

}
