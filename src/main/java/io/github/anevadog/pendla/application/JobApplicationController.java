package io.github.anevadog.pendla.application;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.anevadog.pendla.application.dto.CreateJobApplicationRequest;
import io.github.anevadog.pendla.application.dto.JobApplicationResponse;
import io.github.anevadog.pendla.application.dto.UpdateStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * HTTP layer for job applications. No logic and no try/catch: errors go to GlobalExceptionHandler.
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
			description = "Starts in status SAVED. 400 invalid data · 409 an application for this jobtechId already exists")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public JobApplicationResponse create(@Valid @RequestBody CreateJobApplicationRequest request) {
		return service.create(request);
	}

	@Operation(summary = "List job applications",
			description = "Newest first. Optional filter by status. 400 invalid status")
	@GetMapping
	public List<JobApplicationResponse> findAll(@RequestParam(required = false) ApplicationStatus status) {
		return service.findAll(status);
	}

	@Operation(summary = "Get one job application", description = "404 not found")
	@GetMapping("/{id}")
	public JobApplicationResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@Operation(summary = "Change the status of a job application",
			description = "Allowed: SAVED→SENT, SENT→INTERVIEW|REJECTED, INTERVIEW→OFFER|REJECTED. "
					+ "400 invalid status · 404 not found · 409 transition not allowed")
	@PatchMapping("/{id}/status")
	public JobApplicationResponse changeStatus(@PathVariable Long id,
			@Valid @RequestBody UpdateStatusRequest request) {
		return service.changeStatus(id, request);
	}

	@Operation(summary = "Delete a job application", description = "404 not found")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		service.delete(id);
	}

}
