package io.github.anevadog.pendla.jobsearch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The part of JobTech's GET /search response that Pendla reads. Everything else is ignored,
 * including contact data (application_details, application_contacts), which is never mapped.
 *
 * Field names follow JobTech's JSON; @JsonProperty maps their snake_case names to Java names.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JobTechSearchResponse(List<Hit> hits) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Hit(
			String id,
			String headline,
			Employer employer,
			@JsonProperty("workplace_address") WorkplaceAddress workplaceAddress,
			@JsonProperty("webpage_url") String webpageUrl) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Employer(String name) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record WorkplaceAddress(String municipality) {
	}

}
