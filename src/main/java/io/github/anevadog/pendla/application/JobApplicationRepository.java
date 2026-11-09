package io.github.anevadog.pendla.application;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data generates the implementation from the method names.
 */
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

	boolean existsByJobtechId(String jobtechId);

}
