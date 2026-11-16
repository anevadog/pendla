package io.github.anevadog.pendla.application;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.anevadog.pendla.application.dto.CreateJobApplicationRequest;
import io.github.anevadog.pendla.application.dto.JobApplicationResponse;
import io.github.anevadog.pendla.application.dto.UpdateDetailsRequest;
import io.github.anevadog.pendla.application.dto.UpdateStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * HTTP layer for job applications. No logic and no try/catch: errors go to GlobalExceptionHandler.
 *
 * Spring fills the Authentication parameter with the logged-in user: getName() is the username
 * that came in the token. Each method passes it to the service, which decides what that user can see.
 */
@RestController
@RequestMapping("/api/applications")
@Tag(name = "Applications", description = "Save job applications and track their status")
public class JobApplicationController {

	private final JobApplicationService service;

	public JobApplicationController(JobApplicationService service) {
		this.service = service;
	}

	@Operation(summary = "Save a job application",
			description = "Starts in status SAVED. 400 invalid data · 409 you already saved an application for this jobtechId")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public JobApplicationResponse create(Authentication authentication,
			@Valid @RequestBody CreateJobApplicationRequest request) {
		return service.create(authentication.getName(), request);
	}

	@Operation(summary = "List job applications",
			description = "Only your own, newest first. Optional filter by status. 400 invalid status")
	@GetMapping
	public List<JobApplicationResponse> findAll(Authentication authentication,
			@RequestParam(required = false) ApplicationStatus status) {
		return service.findAll(authentication.getName(), status);
	}

	@Operation(summary = "Get one job application", description = "404 not found")
	@GetMapping("/{id}")
	public JobApplicationResponse findById(Authentication authentication, @PathVariable Long id) {
		return service.findById(authentication.getName(), id);
	}

	@Operation(summary = "Change the status of a job application",
			description = "Allowed: SAVED→SENT, SENT→INTERVIEW|REJECTED, INTERVIEW→OFFER|REJECTED. "
					+ "400 invalid status · 404 not found · 409 transition not allowed")
	@PatchMapping("/{id}/status")
	public JobApplicationResponse changeStatus(Authentication authentication, @PathVariable Long id,
			@Valid @RequestBody UpdateStatusRequest request) {
		return service.changeStatus(authentication.getName(), id, request);
	}

	@Operation(summary = "Replace the notes and interview date of a job application",
			description = "Both fields are replaced: a missing or null field is cleared. "
					+ "Notes must not contain third-party contact data. "
					+ "400 invalid data · 404 not found")
	@PutMapping("/{id}/details")
	public JobApplicationResponse updateDetails(Authentication authentication, @PathVariable Long id,
			@Valid @RequestBody UpdateDetailsRequest request) {
		return service.updateDetails(authentication.getName(), id, request);
	}

	@Operation(summary = "Delete a job application", description = "404 not found")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(Authentication authentication, @PathVariable Long id) {
		service.delete(authentication.getName(), id);
	}

}
