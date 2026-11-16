package io.github.anevadog.pendla.application;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.anevadog.pendla.user.AppUser;

/**
 * Spring Data generates the implementation from the method names.
 * Every query filters by owner, so a user can only reach their own applications.
 */
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

	boolean existsByOwnerAndJobtechId(AppUser owner, String jobtechId);

	Optional<JobApplication> findByIdAndOwner(Long id, AppUser owner);

	List<JobApplication> findAllByOwnerOrderByCreatedAtDesc(AppUser owner);

	List<JobApplication> findAllByOwnerAndStatusOrderByCreatedAtDesc(AppUser owner, ApplicationStatus status);

}
