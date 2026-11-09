package io.github.anevadog.pendla.jobsearch;

import java.net.SocketTimeoutException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import io.github.anevadog.pendla.jobsearch.dto.JobTechSearchResponse;

/**
 * The only class that talks to JobTech. One call per search, no retries.
 *
 * It turns RestClient's exceptions into our own, so the rest of the app never depends on RestClient.
 */
@Component
public class JobTechClient {

	/** Fixed page size: we never ask JobTech for more than this. */
	static final int LIMIT = 20;

	private static final Logger log = LoggerFactory.getLogger(JobTechClient.class);

	private final RestClient restClient;

	public JobTechClient(RestClient jobTechRestClient) {
		this.restClient = jobTechRestClient;
	}

	/**
	 * @param keyword                 free text, sent as q
	 * @param municipalityConceptId   JobTech's municipality id, or null to search the whole country
	 * @throws JobTechTimeoutException     if JobTech does not answer in time
	 * @throws JobTechUnavailableException for any other failure (4xx, 5xx, unreadable body, no connection)
	 */
	public JobTechSearchResponse search(String keyword, String municipalityConceptId) {
		JobTechSearchResponse response;
		try {
			response = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/search")
							.queryParam("q", keyword)
							.queryParamIfPresent("municipality", Optional.ofNullable(municipalityConceptId))
							.queryParam("limit", LIMIT)
							.build())
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.body(JobTechSearchResponse.class);
		} catch (RestClientException ex) {
			log.warn("JobTech request failed: {}", ex.getMessage());
			// Timeouts arrive as a ResourceAccessException whose cause is a SocketTimeoutException
			if (ex.getCause() instanceof SocketTimeoutException) {
				throw new JobTechTimeoutException(ex);
			}
			throw new JobTechUnavailableException(ex);
		}

		if (response == null) {
			log.warn("JobTech returned an empty body");
			throw new JobTechUnavailableException(null);
		}
		return response;
	}

}
