package io.github.anevadog.pendla.application.dto;

import org.hibernate.validator.constraints.URL;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/applications. Only title and employer are required.
 * The @Schema examples are what Swagger UI pre-fills in "Try it out".
 */
public record CreateJobApplicationRequest(

		@Schema(description = "Id of the ad in JobTech (optional). Must be unique", example = "31696992")
		@Size(max = 50)
		String jobtechId,

		@Schema(example = "Integrationsutvecklare Java")
		@NotBlank
		@Size(max = 255)
		String title,

		@Schema(example = "Deploja AB")
		@NotBlank
		@Size(max = 255)
		String employer,

		@Schema(example = "Stockholm")
		@Size(max = 255)
		String city,

		@Schema(example = "https://arbetsformedlingen.se/platsbanken/annonser/31696992")
		@URL
		@Size(max = 2048)
		String url) {
}
