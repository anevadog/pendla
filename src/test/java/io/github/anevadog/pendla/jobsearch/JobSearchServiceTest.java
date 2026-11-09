package io.github.anevadog.pendla.jobsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.anevadog.pendla.jobsearch.dto.JobAdResponse;
import io.github.anevadog.pendla.jobsearch.dto.JobTechSearchResponse;
import io.github.anevadog.pendla.jobsearch.dto.JobTechSearchResponse.Employer;
import io.github.anevadog.pendla.jobsearch.dto.JobTechSearchResponse.Hit;
import io.github.anevadog.pendla.jobsearch.dto.JobTechSearchResponse.WorkplaceAddress;

/**
 * Unit tests: JobTechClient is a Mockito mock, so no HTTP at all.
 */
@ExtendWith(MockitoExtension.class)
class JobSearchServiceTest {

	@Mock
	private JobTechClient client;

	@InjectMocks
	private JobSearchService service;

	private static Hit javaAdInGoteborg() {
		return new Hit("31696992", "Java Developer", new Employer("Deploja AB"),
				new WorkplaceAddress("Göteborg"), "https://arbetsformedlingen.se/platsbanken/annonser/31696992");
	}

	@Test
	void search_withValidCity_returnsMappedAds() {
		when(client.search("java", "PVZL_BQT_XtL"))
				.thenReturn(new JobTechSearchResponse(List.of(javaAdInGoteborg())));

		List<JobAdResponse> ads = service.search("java", "Göteborg");

		assertThat(ads).containsExactly(new JobAdResponse("31696992", "Java Developer", "Deploja AB",
				"Göteborg", "https://arbetsformedlingen.se/platsbanken/annonser/31696992"));
	}

	@Test
	void search_withLowercaseCity_usesMunicipalityId() {
		when(client.search("java", "PVZL_BQT_XtL")).thenReturn(new JobTechSearchResponse(List.of()));

		service.search("java", " göteborg ");

		verify(client).search("java", "PVZL_BQT_XtL");
	}

	@Test
	void search_withoutCity_searchesWholeCountry() {
		when(client.search("java", null)).thenReturn(new JobTechSearchResponse(List.of()));

		service.search(" java ", null);

		verify(client).search("java", null);
	}

	@Test
	void search_withUnknownCity_throwsUnsupportedCityWithoutCallingJobTech() {
		assertThatThrownBy(() -> service.search("java", "Gothenburg"))
				.isInstanceOf(UnsupportedCityException.class)
				.hasMessage("Unsupported city 'Gothenburg'. Supported cities: Stockholm, Göteborg, Malmö, Uppsala");
		verifyNoInteractions(client);
	}

	@Test
	void search_withMissingEmployerAndAddress_mapsNulls() {
		var hitWithoutOptionalData = new Hit("1", "Java Developer", null, null, null);
		when(client.search(anyString(), any())).thenReturn(new JobTechSearchResponse(List.of(hitWithoutOptionalData)));

		List<JobAdResponse> ads = service.search("java", null);

		assertThat(ads).containsExactly(new JobAdResponse("1", "Java Developer", null, null, null));
	}

	@Test
	void search_withNullHits_returnsEmptyList() {
		when(client.search(anyString(), any())).thenReturn(new JobTechSearchResponse(null));

		assertThat(service.search("java", null)).isEmpty();
	}

}
