package io.github.anevadog.pendla.application.dto;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/applications. Only title and employer are required.
 */
public record CreateJobApplicationRequest(

		@Size(max = 50)
		String jobtechId,

		@NotBlank
		@Size(max = 255)
		String title,

		@NotBlank
		@Size(max = 255)
		String employer,

		@Size(max = 255)
		String city,

		@URL
		@Size(max = 2048)
		String url) {
}
