package io.github.anevadog.pendla.jobsearch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.anevadog.pendla.jobsearch.dto.JobAdResponse;

/**
 * Web layer tests: real controller, method validation and GlobalExceptionHandler; the service is a mock.
 */
@WebMvcTest(JobSearchController.class)
class JobSearchControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JobSearchService service;

	@Test
	void searchJobs_validRequest_returns200() throws Exception {
		var ad = new JobAdResponse("31696992", "Java Developer", "Deploja AB", "Göteborg",
				"https://arbetsformedlingen.se/platsbanken/annonser/31696992");
		when(service.search("java", "Göteborg")).thenReturn(List.of(ad));

		mockMvc.perform(get("/api/jobs").param("keyword", "java").param("city", "Göteborg"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].jobtechId").value("31696992"))
				.andExpect(jsonPath("$[0].employer").value("Deploja AB"))
				.andExpect(jsonPath("$[0].city").value("Göteborg"));
	}

	@Test
	void searchJobs_missingKeyword_returns400() throws Exception {
		mockMvc.perform(get("/api/jobs").param("city", "Stockholm"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
		verify(service, never()).search(any(), any());
	}

	@Test
	void searchJobs_blankKeyword_returns400() throws Exception {
		mockMvc.perform(get("/api/jobs").param("keyword", "   "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.keyword").exists());
		verify(service, never()).search(any(), any());
	}

	@Test
	void searchJobs_unknownCity_returns400() throws Exception {
		when(service.search("java", "Gothenburg")).thenThrow(new UnsupportedCityException("Gothenburg"));

		mockMvc.perform(get("/api/jobs").param("keyword", "java").param("city", "Gothenburg"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail")
						.value("Unsupported city 'Gothenburg'. Supported cities: Stockholm, Göteborg, Malmö, Uppsala"));
	}

	@Test
	void searchJobs_jobTechDown_returns502() throws Exception {
		when(service.search("java", null)).thenThrow(new JobTechUnavailableException(null));

		mockMvc.perform(get("/api/jobs").param("keyword", "java"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.detail").value("JobTech is not available right now, try again later"));
	}

	@Test
	void searchJobs_jobTechTimeout_returns504() throws Exception {
		when(service.search("java", null)).thenThrow(new JobTechTimeoutException(null));

		mockMvc.perform(get("/api/jobs").param("keyword", "java"))
				.andExpect(status().isGatewayTimeout())
				.andExpect(jsonPath("$.detail").value("JobTech took too long to answer, try again later"));
	}

}
