package io.github.anevadog.pendla.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import io.github.anevadog.pendla.application.dto.CreateJobApplicationRequest;
import io.github.anevadog.pendla.application.dto.JobApplicationResponse;
import io.github.anevadog.pendla.application.dto.UpdateDetailsRequest;
import io.github.anevadog.pendla.application.dto.UpdateStatusRequest;
import io.github.anevadog.pendla.user.AppUser;
import io.github.anevadog.pendla.user.AppUserRepository;

/**
 * Unit tests: the repositories are Mockito mocks, no Spring context and no database.
 *
 * The logged-in user is always "alex". The mock only answers when it is asked for alex's data,
 * so a test passes only if the service filters by the right owner.
 */
@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

	private final AppUser alex = new AppUser("alex", "hash");

	@Mock
	private JobApplicationRepository repository;

	@Mock
	private AppUserRepository userRepository;

	@InjectMocks
	private JobApplicationService service;

	@BeforeEach
	void alexExists() {
		when(userRepository.findByUsername("alex")).thenReturn(Optional.of(alex));
	}

	@Test
	void create_withNewJobtechId_savesWithStatusSaved() {
		var request = new CreateJobApplicationRequest(
				"31696992", " Java Developer ", "Deploja AB", "Stockholm", "https://example.com/ad");
		when(repository.existsByOwnerAndJobtechId(alex, "31696992")).thenReturn(false);
		// saveAndFlush() returns the same entity it receives
		when(repository.saveAndFlush(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

		JobApplicationResponse response = service.create("alex", request);

		assertThat(response.status()).isEqualTo(ApplicationStatus.SAVED);
		assertThat(response.jobtechId()).isEqualTo("31696992");
		assertThat(response.title()).isEqualTo("Java Developer");
		assertThat(response.employer()).isEqualTo("Deploja AB");
		assertThat(response.createdAt()).isNotNull();
	}

	@Test
	void create_withDuplicateJobtechId_throwsDuplicate() {
		var request = new CreateJobApplicationRequest("31696992", "Java Developer", "Deploja AB", null, null);
		when(repository.existsByOwnerAndJobtechId(alex, "31696992")).thenReturn(true);

		assertThatThrownBy(() -> service.create("alex", request))
				.isInstanceOf(DuplicateJobApplicationException.class)
				.hasMessageContaining("31696992");
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void create_whenUniqueConstraintFails_throwsDuplicate() {
		// Race: the check finds nothing, but another request inserts the same jobtechId before ours
		var request = new CreateJobApplicationRequest("31696992", "Java Developer", "Deploja AB", null, null);
		when(repository.existsByOwnerAndJobtechId(alex, "31696992")).thenReturn(false);
		when(repository.saveAndFlush(any(JobApplication.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate key"));

		assertThatThrownBy(() -> service.create("alex", request))
				.isInstanceOf(DuplicateJobApplicationException.class)
				.hasMessageContaining("31696992");
	}

	@Test
	void create_whenIntegrityFailsWithoutJobtechId_rethrowsOriginal() {
		var request = new CreateJobApplicationRequest(null, "Java Developer", "Deploja AB", null, null);
		when(repository.saveAndFlush(any(JobApplication.class)))
				.thenThrow(new DataIntegrityViolationException("other constraint"));

		assertThatThrownBy(() -> service.create("alex", request))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void create_withoutJobtechId_skipsDuplicateCheck() {
		var request = new CreateJobApplicationRequest(null, "Java Developer", "Deploja AB", null, null);
		when(repository.saveAndFlush(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

		JobApplicationResponse response = service.create("alex", request);

		assertThat(response.jobtechId()).isNull();
		verify(repository, never()).existsByOwnerAndJobtechId(any(), any());
	}

	@Test
	void create_withBlankOptionalFields_storesNull() {
		var request = new CreateJobApplicationRequest("  ", "Java Developer", "Deploja AB", "", " ");
		when(repository.saveAndFlush(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

		JobApplicationResponse response = service.create("alex", request);

		assertThat(response.jobtechId()).isNull();
		assertThat(response.city()).isNull();
		assertThat(response.url()).isNull();
		verify(repository, never()).existsByOwnerAndJobtechId(any(), any());
	}

	@Test
	void create_whenUserOfTokenDoesNotExist_throwsUsernameNotFound() {
		var request = new CreateJobApplicationRequest(null, "Java Developer", "Deploja AB", null, null);
		when(userRepository.findByUsername("alex")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create("alex", request))
				.isInstanceOf(UsernameNotFoundException.class);
		verifyNoInteractions(repository);
	}

	@Test
	void findAll_returnsOwnApplicationsInRepositoryOrder() {
		var newest = new JobApplication(alex, null, "Backend Developer", "Spotify", "Stockholm", null);
		var oldest = new JobApplication(alex, null, "Java Developer", "Deploja AB", "Stockholm", null);
		when(repository.findAllByOwnerOrderByCreatedAtDesc(alex)).thenReturn(List.of(newest, oldest));

		List<JobApplicationResponse> responses = service.findAll("alex", null);

		assertThat(responses)
				.extracting(JobApplicationResponse::title)
				.containsExactly("Backend Developer", "Java Developer");
		verify(repository, never()).findAllByOwnerAndStatusOrderByCreatedAtDesc(any(), any());
		// The unfiltered query of JpaRepository would return everybody's applications
		verify(repository, never()).findAll();
	}

	@Test
	void findAll_withNoApplications_returnsEmptyList() {
		when(repository.findAllByOwnerOrderByCreatedAtDesc(alex)).thenReturn(List.of());

		assertThat(service.findAll("alex", null)).isEmpty();
	}

	@Test
	void findAll_withStatus_usesFilteredQuery() {
		var saved = new JobApplication(alex, null, "Java Developer", "Deploja AB", "Stockholm", null);
		when(repository.findAllByOwnerAndStatusOrderByCreatedAtDesc(alex, ApplicationStatus.SAVED))
				.thenReturn(List.of(saved));

		List<JobApplicationResponse> responses = service.findAll("alex", ApplicationStatus.SAVED);

		assertThat(responses)
				.extracting(JobApplicationResponse::title)
				.containsExactly("Java Developer");
		verify(repository, never()).findAllByOwnerOrderByCreatedAtDesc(any());
	}

	@Test
	void findById_withOwnApplication_returnsResponse() {
		var application = new JobApplication(alex, "31696992", "Java Developer", "Deploja AB", "Stockholm", null);
		when(repository.findByIdAndOwner(1L, alex)).thenReturn(Optional.of(application));

		JobApplicationResponse response = service.findById("alex", 1L);

		assertThat(response.jobtechId()).isEqualTo("31696992");
		assertThat(response.title()).isEqualTo("Java Developer");
		assertThat(response.status()).isEqualTo(ApplicationStatus.SAVED);
	}

	// In the four "unknown or another user's" tests below, the repository finds nothing for (id, alex).
	// That is what the real query returns in both cases, so the service cannot tell them apart.

	@Test
	void findById_withUnknownIdOrAnotherUsersApplication_throwsNotFound() {
		when(repository.findByIdAndOwner(99L, alex)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findById("alex", 99L))
				.isInstanceOf(JobApplicationNotFoundException.class)
				.hasMessageContaining("99");
		// The query without owner would find another user's application
		verify(repository, never()).findById(any());
	}

	@Test
	void changeStatus_validTransition_returnsUpdatedStatus() {
		var application = new JobApplication(alex, null, "Java Developer", "Deploja AB", null, null);
		when(repository.findByIdAndOwner(1L, alex)).thenReturn(Optional.of(application));

		JobApplicationResponse response = service.changeStatus("alex", 1L,
				new UpdateStatusRequest(ApplicationStatus.SENT));

		assertThat(response.status()).isEqualTo(ApplicationStatus.SENT);
		// Dirty checking saves the change: the service must not call save()
		verify(repository, never()).save(any());
	}

	@Test
	void changeStatus_invalidTransition_throwsInvalidStatusTransitionException() {
		var application = new JobApplication(alex, null, "Java Developer", "Deploja AB", null, null);
		when(repository.findByIdAndOwner(1L, alex)).thenReturn(Optional.of(application));

		assertThatThrownBy(() -> service.changeStatus("alex", 1L, new UpdateStatusRequest(ApplicationStatus.OFFER)))
				.isInstanceOf(InvalidStatusTransitionException.class)
				.hasMessage("Cannot change status from SAVED to OFFER");
	}

	@Test
	void changeStatus_withUnknownIdOrAnotherUsersApplication_throwsNotFound() {
		when(repository.findByIdAndOwner(99L, alex)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.changeStatus("alex", 99L, new UpdateStatusRequest(ApplicationStatus.SENT)))
				.isInstanceOf(JobApplicationNotFoundException.class)
				.hasMessageContaining("99");
		verify(repository, never()).findById(any());
	}

	@Test
	void updateDetails_withOwnApplication_returnsUpdatedResponse() {
		var application = new JobApplication(alex, null, "Java Developer", "Deploja AB", null, null);
		when(repository.findByIdAndOwner(1L, alex)).thenReturn(Optional.of(application));
		Instant interviewAt = Instant.parse("2026-11-20T09:00:00Z");

		JobApplicationResponse response = service.updateDetails("alex", 1L,
				new UpdateDetailsRequest(" Prepare Spring questions ", interviewAt));

		assertThat(response.notes()).isEqualTo("Prepare Spring questions");
		assertThat(response.interviewAt()).isEqualTo(interviewAt);
		// Dirty checking saves the change: the service must not call save()
		verify(repository, never()).save(any());
	}

	@Test
	void updateDetails_withBlankNotes_storesNull() {
		var application = new JobApplication(alex, null, "Java Developer", "Deploja AB", null, null);
		when(repository.findByIdAndOwner(1L, alex)).thenReturn(Optional.of(application));

		JobApplicationResponse response = service.updateDetails("alex", 1L, new UpdateDetailsRequest("   ", null));

		assertThat(response.notes()).isNull();
		assertThat(response.interviewAt()).isNull();
	}

	@Test
	void updateDetails_withUnknownIdOrAnotherUsersApplication_throwsNotFound() {
		when(repository.findByIdAndOwner(99L, alex)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.updateDetails("alex", 99L, new UpdateDetailsRequest("notes", null)))
				.isInstanceOf(JobApplicationNotFoundException.class)
				.hasMessageContaining("99");
		verify(repository, never()).findById(any());
	}

	@Test
	void delete_withOwnApplication_deletes() {
		var application = new JobApplication(alex, null, "Java Developer", "Deploja AB", null, null);
		when(repository.findByIdAndOwner(1L, alex)).thenReturn(Optional.of(application));

		service.delete("alex", 1L);

		verify(repository).delete(application);
	}

	@Test
	void delete_withUnknownIdOrAnotherUsersApplication_throwsNotFoundAndDeletesNothing() {
		when(repository.findByIdAndOwner(99L, alex)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete("alex", 99L))
				.isInstanceOf(JobApplicationNotFoundException.class)
				.hasMessageContaining("99");
		verify(repository, never()).delete(any());
		// deleteById has no owner: it would delete another user's application
		verify(repository, never()).deleteById(any());
	}

}
