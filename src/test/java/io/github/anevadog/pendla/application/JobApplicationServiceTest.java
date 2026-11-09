package io.github.anevadog.pendla.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.anevadog.pendla.application.dto.CreateJobApplicationRequest;
import io.github.anevadog.pendla.application.dto.JobApplicationResponse;

/**
 * Unit tests: the repository is a Mockito mock, no Spring context and no database.
 */
@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

	@Mock
	private JobApplicationRepository repository;

	@InjectMocks
	private JobApplicationService service;

	@Test
	void create_withNewJobtechId_savesWithStatusSaved() {
		var request = new CreateJobApplicationRequest(
				"31696992", " Java Developer ", "Deploja AB", "Stockholm", "https://example.com/ad");
		when(repository.existsByJobtechId("31696992")).thenReturn(false);
		// save() returns the same entity it receives
		when(repository.save(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

		JobApplicationResponse response = service.create(request);

		assertThat(response.status()).isEqualTo(ApplicationStatus.SAVED);
		assertThat(response.jobtechId()).isEqualTo("31696992");
		assertThat(response.title()).isEqualTo("Java Developer");
		assertThat(response.employer()).isEqualTo("Deploja AB");
		assertThat(response.createdAt()).isNotNull();
	}

	@Test
	void create_withDuplicateJobtechId_throwsDuplicate() {
		var request = new CreateJobApplicationRequest("31696992", "Java Developer", "Deploja AB", null, null);
		when(repository.existsByJobtechId("31696992")).thenReturn(true);

		assertThatThrownBy(() -> service.create(request))
				.isInstanceOf(DuplicateJobApplicationException.class)
				.hasMessageContaining("31696992");
		verify(repository, never()).save(any());
	}

	@Test
	void create_withoutJobtechId_skipsDuplicateCheck() {
		var request = new CreateJobApplicationRequest(null, "Java Developer", "Deploja AB", null, null);
		when(repository.save(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

		JobApplicationResponse response = service.create(request);

		assertThat(response.jobtechId()).isNull();
		verify(repository, never()).existsByJobtechId(anyString());
	}

	@Test
	void create_withBlankOptionalFields_storesNull() {
		var request = new CreateJobApplicationRequest("  ", "Java Developer", "Deploja AB", "", " ");
		when(repository.save(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

		JobApplicationResponse response = service.create(request);

		assertThat(response.jobtechId()).isNull();
		assertThat(response.city()).isNull();
		assertThat(response.url()).isNull();
		verify(repository, never()).existsByJobtechId(anyString());
	}

	@Test
	void findAll_returnsResponsesInRepositoryOrder() {
		var newest = new JobApplication(null, "Backend Developer", "Spotify", "Stockholm", null);
		var oldest = new JobApplication(null, "Java Developer", "Deploja AB", "Stockholm", null);
		when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(newest, oldest));

		List<JobApplicationResponse> responses = service.findAll();

		assertThat(responses)
				.extracting(JobApplicationResponse::title)
				.containsExactly("Backend Developer", "Java Developer");
	}

	@Test
	void findAll_withNoApplications_returnsEmptyList() {
		when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

		assertThat(service.findAll()).isEmpty();
	}

}
