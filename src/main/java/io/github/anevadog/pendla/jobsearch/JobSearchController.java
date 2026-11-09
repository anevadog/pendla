package io.github.anevadog.pendla.jobsearch;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.anevadog.pendla.jobsearch.dto.JobAdResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * HTTP layer for the JobTech search. No logic and no try/catch: errors go to GlobalExceptionHandler.
 *
 * The constraints on the parameters are checked by Spring MVC before the method runs (method validation).
 */
@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Job search", description = "Search real job ads in the JobTech JobSearch API")
public class JobSearchController {

	private final JobSearchService service;

	public JobSearchController(JobSearchService service) {
		this.service = service;
	}

	@Operation(summary = "Search job ads",
			description = "One call to JobTech, max 20 results. "
					+ "400 invalid keyword or city · 502 JobTech error · 504 JobTech timeout")
	@GetMapping
	public List<JobAdResponse> search(
			@Parameter(description = "Free text, e.g. a technology or a job title", example = "java")
			@RequestParam @NotBlank @Size(max = 100) String keyword,
			@Parameter(description = "Optional. One of: Stockholm, Göteborg, Malmö, Uppsala (case-insensitive)",
					example = "Göteborg")
			@RequestParam(required = false) @Size(max = 100) String city) {
		return service.search(keyword, city);
	}

}
