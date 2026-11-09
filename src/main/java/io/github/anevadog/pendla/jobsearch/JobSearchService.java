package io.github.anevadog.pendla.jobsearch;

import java.util.List;

import org.springframework.stereotype.Service;

import io.github.anevadog.pendla.jobsearch.dto.JobAdResponse;
import io.github.anevadog.pendla.jobsearch.dto.JobTechSearchResponse;

/**
 * Turns a search request into one JobTech call and maps the result to our simplified ads.
 */
@Service
public class JobSearchService {

	private final JobTechClient client;

	public JobSearchService(JobTechClient client) {
		this.client = client;
	}

	/**
	 * @param keyword free text, already validated as not blank by the controller
	 * @param city    a SupportedCity name, or null/blank to search the whole country
	 * @throws UnsupportedCityException if the city is not supported (checked before calling JobTech)
	 */
	public List<JobAdResponse> search(String keyword, String city) {
		String municipalityConceptId = null;
		if (city != null && !city.isBlank()) {
			municipalityConceptId = SupportedCity.fromName(city)
					.orElseThrow(() -> new UnsupportedCityException(city))
					.getMunicipalityConceptId();
		}

		JobTechSearchResponse response = client.search(keyword.trim(), municipalityConceptId);

		if (response.hits() == null) {
			return List.of();
		}
		return response.hits().stream()
				.map(JobAdResponse::from)
				.toList();
	}

}
