package io.github.anevadog.pendla.application;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.anevadog.pendla.application.dto.CreateJobApplicationRequest;
import io.github.anevadog.pendla.application.dto.JobApplicationResponse;
import io.github.anevadog.pendla.application.dto.UpdateDetailsRequest;
import io.github.anevadog.pendla.application.dto.UpdateStatusRequest;
import io.github.anevadog.pendla.user.AppUser;
import io.github.anevadog.pendla.user.AppUserRepository;

/**
 * Business logic for job applications. Receives and returns DTOs; entities stay inside.
 *
 * Every method receives the username of the logged-in user and only works with that user's
 * applications. An application of another user is treated exactly like one that does not exist.
 */
@Service
public class JobApplicationService {

	private final JobApplicationRepository repository;
	private final AppUserRepository userRepository;

	public JobApplicationService(JobApplicationRepository repository, AppUserRepository userRepository) {
		this.repository = repository;
		this.userRepository = userRepository;
	}

	@Transactional
	public JobApplicationResponse create(String username, CreateJobApplicationRequest request) {
		AppUser owner = findOwner(username);
		String jobtechId = blankToNull(request.jobtechId());

		if (jobtechId != null && repository.existsByOwnerAndJobtechId(owner, jobtechId)) {
			throw new DuplicateJobApplicationException(jobtechId);
		}

		JobApplication application = new JobApplication(
				owner,
				jobtechId,
				request.title().trim(),
				request.employer().trim(),
				blankToNull(request.city()),
				blankToNull(request.url()));

		try {
			// saveAndFlush runs the INSERT now, so a database error surfaces here and not at commit
			return JobApplicationResponse.from(repository.saveAndFlush(application));
		} catch (DataIntegrityViolationException ex) {
			// Two requests with the same jobtechId at the same time: both pass the check above,
			// and the unique constraint rejects the second INSERT. Same answer as the check: 409.
			if (jobtechId != null) {
				throw new DuplicateJobApplicationException(jobtechId);
			}
			throw ex;
		}
	}

	/**
	 * The user's applications, newest first. A null status means no filter.
	 * readOnly tells Hibernate it does not need to track changes.
	 */
	@Transactional(readOnly = true)
	public List<JobApplicationResponse> findAll(String username, ApplicationStatus status) {
		AppUser owner = findOwner(username);
		List<JobApplication> applications = (status == null)
				? repository.findAllByOwnerOrderByCreatedAtDesc(owner)
				: repository.findAllByOwnerAndStatusOrderByCreatedAtDesc(owner, status);

		return applications.stream()
				.map(JobApplicationResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public JobApplicationResponse findById(String username, Long id) {
		return JobApplicationResponse.from(findOwnApplication(username, id));
	}

	@Transactional
	public JobApplicationResponse changeStatus(String username, Long id, UpdateStatusRequest request) {
		JobApplication application = findOwnApplication(username, id);

		application.changeStatus(request.status());

		// No save() needed: the entity is managed inside this transaction,
		// so Hibernate detects the change (dirty checking) and runs the UPDATE on commit.
		return JobApplicationResponse.from(application);
	}

	/** Replaces notes and interview date. Saved by dirty checking, like changeStatus. */
	@Transactional
	public JobApplicationResponse updateDetails(String username, Long id, UpdateDetailsRequest request) {
		JobApplication application = findOwnApplication(username, id);

		application.updateDetails(blankToNull(request.notes()), request.interviewAt());

		return JobApplicationResponse.from(application);
	}

	@Transactional
	public void delete(String username, Long id) {
		repository.delete(findOwnApplication(username, id));
	}

	/**
	 * The single place where "is this application yours?" is decided.
	 * The query filters by id and owner together, so an application of another user is simply
	 * not found: same 404 as an id that does not exist, and nobody learns which ids are in use.
	 */
	private JobApplication findOwnApplication(String username, Long id) {
		return repository.findByIdAndOwner(id, findOwner(username))
				.orElseThrow(() -> new JobApplicationNotFoundException(id));
	}

	/**
	 * The username comes from a token that Spring Security has already validated.
	 * It can still be missing here if the user was removed after the token was issued:
	 * UsernameNotFoundException is an AuthenticationException, so the answer is the usual 401.
	 */
	private AppUser findOwner(String username) {
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("No user for this token"));
	}

	/** Optional fields: store "" or "   " as null, so empty values mean the same thing everywhere. */
	private static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.trim();
	}

}
