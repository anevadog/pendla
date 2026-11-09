package io.github.anevadog.pendla.jobsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import io.github.anevadog.pendla.jobsearch.dto.JobTechSearchResponse;

/**
 * MockRestServiceServer replaces the real HTTP connection: no network and no Spring context.
 * Each test says which request it expects and what JobTech "answers".
 */
class JobTechClientTest {

	private static final String BASE_URL = "https://jobtech.test";

	// Trimmed copy of a real JobTech hit, with an extra field and a contact field we must ignore
	private static final String ONE_HIT = """
			{
			  "total": {"value": 1},
			  "hits": [{
			    "id": "31696992",
			    "headline": "Integrationsutvecklare Java",
			    "employer": {"name": "Deploja AB", "organization_number": "5590000000"},
			    "workplace_address": {"municipality": "Stockholm", "municipality_concept_id": "AvNB_uwa_6n6"},
			    "webpage_url": "https://arbetsformedlingen.se/platsbanken/annonser/31696992",
			    "application_details": {"email": "recruiter@example.com"}
			  }]
			}
			""";

	private MockRestServiceServer server;
	private JobTechClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		client = new JobTechClient(builder.build());
	}

	@Test
	void search_withSuccessfulResponse_returnsHits() {
		server.expect(requestTo(BASE_URL + "/search?q=java&limit=20"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(ONE_HIT, MediaType.APPLICATION_JSON));

		JobTechSearchResponse response = client.search("java", null);

		assertThat(response.hits()).hasSize(1);
		JobTechSearchResponse.Hit hit = response.hits().get(0);
		assertThat(hit.id()).isEqualTo("31696992");
		assertThat(hit.headline()).isEqualTo("Integrationsutvecklare Java");
		assertThat(hit.employer().name()).isEqualTo("Deploja AB");
		assertThat(hit.workplaceAddress().municipality()).isEqualTo("Stockholm");
		assertThat(hit.webpageUrl()).isEqualTo("https://arbetsformedlingen.se/platsbanken/annonser/31696992");
		server.verify();
	}

	@Test
	void search_withCityFilter_sendsMunicipalityParam() {
		server.expect(requestTo(BASE_URL + "/search?q=java&municipality=PVZL_BQT_XtL&limit=20"))
				.andExpect(queryParam("municipality", "PVZL_BQT_XtL"))
				.andExpect(queryParam("limit", "20"))
				.andRespond(withSuccess("{\"hits\": []}", MediaType.APPLICATION_JSON));

		JobTechSearchResponse response = client.search("java", "PVZL_BQT_XtL");

		assertThat(response.hits()).isEmpty();
		server.verify();
	}

	@Test
	void search_withServerError_throwsUnavailable() {
		server.expect(requestTo(BASE_URL + "/search?q=java&limit=20"))
				.andRespond(withServerError());

		assertThatThrownBy(() -> client.search("java", null))
				.isInstanceOf(JobTechUnavailableException.class);
	}

	@Test
	void search_withUnreadableBody_throwsUnavailable() {
		server.expect(requestTo(BASE_URL + "/search?q=java&limit=20"))
				.andRespond(withSuccess("{ this is not json", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.search("java", null))
				.isInstanceOf(JobTechUnavailableException.class);
	}

	@Test
	void search_withConnectionError_throwsUnavailable() {
		server.expect(requestTo(BASE_URL + "/search?q=java&limit=20"))
				.andRespond(request -> {
					throw new ConnectException("Connection refused");
				});

		assertThatThrownBy(() -> client.search("java", null))
				.isInstanceOf(JobTechUnavailableException.class);
	}

	@Test
	void search_withTimeout_throwsTimeout() {
		// Simulates what the real request factory throws when the read timeout expires
		server.expect(requestTo(BASE_URL + "/search?q=java&limit=20"))
				.andRespond(request -> {
					throw new SocketTimeoutException("Read timed out");
				});

		assertThatThrownBy(() -> client.search("java", null))
				.isInstanceOf(JobTechTimeoutException.class);
	}

}
