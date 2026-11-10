package io.github.anevadog.pendla.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import io.github.anevadog.pendla.application.dto.CreateJobApplicationRequest;
import io.github.anevadog.pendla.application.dto.JobApplicationResponse;
import io.github.anevadog.pendla.application.dto.UpdateStatusRequest;

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
		// saveAndFlush() returns the same entity it receives
		when(repository.saveAndFlush(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void create_whenUniqueConstraintFails_throwsDuplicate() {
		// Race: the check finds nothing, but another request inserts the same jobtechId before ours
		var request = new CreateJobApplicationRequest("31696992", "Java Developer", "Deploja AB", null, null);
		when(repository.existsByJobtechId("31696992")).thenReturn(false);
		when(repository.saveAndFlush(any(JobApplication.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate key"));

		assertThatThrownBy(() -> service.create(request))
				.isInstanceOf(DuplicateJobApplicationException.class)
				.hasMessageContaining("31696992");
	}

	@Test
	void create_whenIntegrityFailsWithoutJobtechId_rethrowsOriginal() {
		var request = new CreateJobApplicationRequest(null, "Java Developer", "Deploja AB", null, null);
		when(repository.saveAndFlush(any(JobApplication.class)))
				.thenThrow(new DataIntegrityViolationException("other constraint"));

		assertThatThrownBy(() -> service.create(request))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void create_withoutJobtechId_skipsDuplicateCheck() {
		var request = new CreateJobApplicationRequest(null, "Java Developer", "Deploja AB", null, null);
		when(repository.saveAndFlush(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

		JobApplicationResponse response = service.create(request);

		assertThat(response.jobtechId()).isNull();
		verify(repository, never()).existsByJobtechId(anyString());
	}

	@Test
	void create_withBlankOptionalFields_storesNull() {
		var request = new CreateJobApplicationRequest("  ", "Java Developer", "Deploja AB", "", " ");
		when(repository.saveAndFlush(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

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

		List<JobApplicationResponse> responses = service.findAll(null);

		assertThat(responses)
				.extracting(JobApplicationResponse::title)
				.containsExactly("Backend Developer", "Java Developer");
		verify(repository, never()).findAllByStatusOrderByCreatedAtDesc(any());
	}

	@Test
	void findAll_withNoApplications_returnsEmptyList() {
		when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

		assertThat(service.findAll(null)).isEmpty();
	}

	@Test
	void findAll_withStatus_usesFilteredQuery() {
		var saved = new JobApplication(null, "Java Developer", "Deploja AB", "Stockholm", null);
		when(repository.findAllByStatusOrderByCreatedAtDesc(ApplicationStatus.SAVED)).thenReturn(List.of(saved));

		List<JobApplicationResponse> responses = service.findAll(ApplicationStatus.SAVED);

		assertThat(responses)
				.extracting(JobApplicationResponse::title)
				.containsExactly("Java Developer");
		verify(repository, never()).findAllByOrderByCreatedAtDesc();
	}

	@Test
	void findById_withExistingId_returnsResponse() {
		var application = new JobApplication("31696992", "Java Developer", "Deploja AB", "Stockholm", null);
		when(repository.findById(1L)).thenReturn(Optional.of(application));

		JobApplicationResponse response = service.findById(1L);

		assertThat(response.jobtechId()).isEqualTo("31696992");
		assertThat(response.title()).isEqualTo("Java Developer");
		assertThat(response.status()).isEqualTo(ApplicationStatus.SAVED);
	}

	@Test
	void findById_withUnknownId_throwsNotFound() {
		when(repository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findById(99L))
				.isInstanceOf(JobApplicationNotFoundException.class)
				.hasMessageContaining("99");
	}

	@Test
	void changeStatus_validTransition_returnsUpdatedStatus() {
		var application = new JobApplication(null, "Java Developer", "Deploja AB", null, null);
		when(repository.findById(1L)).thenReturn(Optional.of(application));

		JobApplicationResponse response = service.changeStatus(1L, new UpdateStatusRequest(ApplicationStatus.SENT));

		assertThat(response.status()).isEqualTo(ApplicationStatus.SENT);
		// Dirty checking saves the change: the service must not call save()
		verify(repository, never()).save(any());
	}

	@Test
	void changeStatus_invalidTransition_throwsInvalidStatusTransitionException() {
		var application = new JobApplication(null, "Java Developer", "Deploja AB", null, null);
		when(repository.findById(1L)).thenReturn(Optional.of(application));

		assertThatThrownBy(() -> service.changeStatus(1L, new UpdateStatusRequest(ApplicationStatus.OFFER)))
				.isInstanceOf(InvalidStatusTransitionException.class)
				.hasMessage("Cannot change status from SAVED to OFFER");
	}

	@Test
	void changeStatus_unknownId_throwsNotFound() {
		when(repository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.changeStatus(99L, new UpdateStatusRequest(ApplicationStatus.SENT)))
				.isInstanceOf(JobApplicationNotFoundException.class)
				.hasMessageContaining("99");
	}

	@Test
	void delete_withExistingId_deletes() {
		when(repository.existsById(1L)).thenReturn(true);

		service.delete(1L);

		verify(repository).deleteById(1L);
	}

	@Test
	void delete_withUnknownId_throwsNotFound() {
		when(repository.existsById(99L)).thenReturn(false);

		assertThatThrownBy(() -> service.delete(99L))
				.isInstanceOf(JobApplicationNotFoundException.class)
				.hasMessageContaining("99");
		verify(repository, never()).deleteById(any());
	}

}
