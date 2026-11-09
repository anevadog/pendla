package io.github.anevadog.pendla.jobsearch;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.anevadog.pendla.jobsearch.dto.JobAdResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * HTTP layer for the JobTech search. No logic and no try/catch: errors go to GlobalExceptionHandler.
 *
 * The constraints on the parameters are checked by Spring MVC before the method runs (method validation).
 */
@RestController
@RequestMapping("/api/jobs")
public class JobSearchController {

	private final JobSearchService service;

	public JobSearchController(JobSearchService service) {
		this.service = service;
	}

	@GetMapping
	public List<JobAdResponse> search(
			@RequestParam @NotBlank @Size(max = 100) String keyword,
			@RequestParam(required = false) @Size(max = 100) String city) {
		return service.search(keyword, city);
	}

}
