package io.github.anevadog.pendla.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.anevadog.pendla.application.dto.JobApplicationResponse;

/**
 * Web layer tests: real controller, validation and GlobalExceptionHandler; the service is a mock.
 */
@WebMvcTest(JobApplicationController.class)
class JobApplicationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JobApplicationService service;

	@Test
	void createApplication_withValidBody_returns201() throws Exception {
		var saved = new JobApplicationResponse(1L, "31696992", "Java Developer", "Deploja AB",
				"Stockholm", "https://example.com/ad", ApplicationStatus.SAVED, Instant.parse("2026-11-09T10:00:00Z"));
		when(service.create(any())).thenReturn(saved);

		mockMvc.perform(post("/api/applications")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"jobtechId": "31696992", "title": "Java Developer", "employer": "Deploja AB",
						 "city": "Stockholm", "url": "https://example.com/ad"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.status").value("SAVED"))
				.andExpect(jsonPath("$.title").value("Java Developer"));
	}

	@Test
	void createApplication_withBlankTitle_returns400() throws Exception {
		mockMvc.perform(post("/api/applications")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title": "  ", "employer": "Deploja AB"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.title").exists());
		verify(service, never()).create(any());
	}

	@Test
	void createApplication_withInvalidUrl_returns400() throws Exception {
		mockMvc.perform(post("/api/applications")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title": "Java Developer", "employer": "Deploja AB", "url": "not a url"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.url").exists());
	}

	@Test
	void createApplication_withMalformedJson_returns400() throws Exception {
		mockMvc.perform(post("/api/applications")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\": "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void createApplication_withDuplicateJobtechId_returns409() throws Exception {
		when(service.create(any())).thenThrow(new DuplicateJobApplicationException("31696992"));

		mockMvc.perform(post("/api/applications")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"jobtechId": "31696992", "title": "Java Developer", "employer": "Deploja AB"}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("An application for JobTech ad 31696992 already exists"));
	}

	@Test
	void listApplications_returns200WithList() throws Exception {
		var first = new JobApplicationResponse(2L, null, "Backend Developer", "Spotify",
				"Stockholm", null, ApplicationStatus.SENT, Instant.parse("2026-11-09T12:00:00Z"));
		var second = new JobApplicationResponse(1L, "31696992", "Java Developer", "Deploja AB",
				"Stockholm", null, ApplicationStatus.SAVED, Instant.parse("2026-11-09T10:00:00Z"));
		when(service.findAll()).thenReturn(List.of(first, second));

		mockMvc.perform(get("/api/applications"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(2))
				.andExpect(jsonPath("$[1].id").value(1));
	}

	@Test
	void listApplications_withNoApplications_returnsEmptyArray() throws Exception {
		when(service.findAll()).thenReturn(List.of());

		mockMvc.perform(get("/api/applications"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void getApplication_withExistingId_returns200() throws Exception {
		var found = new JobApplicationResponse(1L, "31696992", "Java Developer", "Deploja AB",
				"Stockholm", null, ApplicationStatus.SENT, Instant.parse("2026-11-09T10:00:00Z"));
		when(service.findById(1L)).thenReturn(found);

		mockMvc.perform(get("/api/applications/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.title").value("Java Developer"))
				.andExpect(jsonPath("$.status").value("SENT"));
	}

	@Test
	void getApplication_withUnknownId_returns404() throws Exception {
		when(service.findById(99L)).thenThrow(new JobApplicationNotFoundException(99L));

		mockMvc.perform(get("/api/applications/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Job application 99 not found"));
	}

	@Test
	void getApplication_withNonNumericId_returns400() throws Exception {
		mockMvc.perform(get("/api/applications/abc"))
				.andExpect(status().isBadRequest());
		verify(service, never()).findById(any());
	}

	@Test
	void changeStatus_validTransition_returns200() throws Exception {
		var updated = new JobApplicationResponse(1L, null, "Java Developer", "Deploja AB",
				null, null, ApplicationStatus.SENT, Instant.parse("2026-11-09T10:00:00Z"));
		when(service.changeStatus(eq(1L), any())).thenReturn(updated);

		mockMvc.perform(patch("/api/applications/1/status")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status": "SENT"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.status").value("SENT"));
	}

	@Test
	void changeStatus_unknownId_returns404() throws Exception {
		when(service.changeStatus(eq(99L), any())).thenThrow(new JobApplicationNotFoundException(99L));

		mockMvc.perform(patch("/api/applications/99/status")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status": "SENT"}
						"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Job application 99 not found"));
	}

	@Test
	void changeStatus_missingStatus_returns400() throws Exception {
		mockMvc.perform(patch("/api/applications/1/status")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.status").exists());
		verify(service, never()).changeStatus(any(), any());
	}

	@Test
	void changeStatus_unknownStatusValue_returns400() throws Exception {
		mockMvc.perform(patch("/api/applications/1/status")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status": "FOO"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
		verify(service, never()).changeStatus(any(), any());
	}

	@Test
	void changeStatus_invalidTransition_returns409() throws Exception {
		when(service.changeStatus(eq(1L), any())).thenThrow(
				new InvalidStatusTransitionException(ApplicationStatus.REJECTED, ApplicationStatus.SENT));

		mockMvc.perform(patch("/api/applications/1/status")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status": "SENT"}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Cannot change status from REJECTED to SENT"));
	}

	@Test
	void deleteApplication_returns204() throws Exception {
		mockMvc.perform(delete("/api/applications/1"))
				.andExpect(status().isNoContent());
		verify(service).delete(1L);
	}

	@Test
	void deleteApplication_withUnknownId_returns404() throws Exception {
		doThrow(new JobApplicationNotFoundException(99L)).when(service).delete(99L);

		mockMvc.perform(delete("/api/applications/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Job application 99 not found"));
	}

	@Test
	void deleteApplication_withNonNumericId_returns400() throws Exception {
		mockMvc.perform(delete("/api/applications/abc"))
				.andExpect(status().isBadRequest());
		verify(service, never()).delete(any());
	}

}
