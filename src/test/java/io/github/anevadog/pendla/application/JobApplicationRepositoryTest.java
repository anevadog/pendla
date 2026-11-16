package io.github.anevadog.pendla.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import jakarta.persistence.EntityManager;

/**
 * The only test that uses a real database: PostgreSQL in a Docker container (Testcontainers).
 * When the context starts, Flyway runs the migrations and Hibernate validates the schema,
 * so a migration that does not match the entity makes every test here fail.
 *
 * Skipped when Docker is not running, so the other tests still run without it.
 * Each test runs in a transaction that is rolled back at the end (@DataJpaTest).
 */
@DataJpaTest
// Use the datasource configured below instead of looking for an in-memory database
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class JobApplicationRepositoryTest {

	// static: one container for all the tests in this class
	@Container
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

	// The container gets a random port, so the connection data is only known once it has started
	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private JobApplicationRepository repository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void save_withAllFields_readsBackTheSameValues() {
		Instant interviewAt = Instant.parse("2026-11-20T09:00:00Z");
		JobApplication application = newApplication("31696992");
		application.updateDetails("Prepare Spring questions", interviewAt);

		Long id = repository.saveAndFlush(application).getId();
		// Empty the persistence context so the next read really comes from the database
		entityManager.clear();

		JobApplication found = repository.findById(id).orElseThrow();
		assertThat(found.getJobtechId()).isEqualTo("31696992");
		assertThat(found.getTitle()).isEqualTo("Java Developer");
		assertThat(found.getEmployer()).isEqualTo("Deploja AB");
		assertThat(found.getCity()).isEqualTo("Stockholm");
		assertThat(found.getStatus()).isEqualTo(ApplicationStatus.SAVED);
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getNotes()).isEqualTo("Prepare Spring questions");
		assertThat(found.getInterviewAt()).isEqualTo(interviewAt);
	}

	@Test
	void save_withDuplicateJobtechId_throwsDataIntegrityViolation() {
		repository.saveAndFlush(newApplication("31696992"));

		assertThatThrownBy(() -> repository.saveAndFlush(newApplication("31696992")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void save_withTwoNullJobtechIds_savesBoth() {
		repository.saveAndFlush(newApplication(null));
		repository.saveAndFlush(newApplication(null));

		assertThat(repository.count()).isEqualTo(2);
	}

	@Test
	void existsByJobtechId_withSavedAndUnknownId_returnsTrueAndFalse() {
		repository.saveAndFlush(newApplication("31696992"));

		assertThat(repository.existsByJobtechId("31696992")).isTrue();
		assertThat(repository.existsByJobtechId("00000000")).isFalse();
	}

	@Test
	void findAllByStatusOrderByCreatedAtDesc_withSent_returnsOnlySentApplications() {
		JobApplication sent = newApplication("1");
		sent.changeStatus(ApplicationStatus.SENT);
		repository.saveAndFlush(sent);
		repository.saveAndFlush(newApplication("2"));

		List<JobApplication> found = repository.findAllByStatusOrderByCreatedAtDesc(ApplicationStatus.SENT);

		assertThat(found).extracting(JobApplication::getJobtechId).containsExactly("1");
	}

	private JobApplication newApplication(String jobtechId) {
		return new JobApplication(jobtechId, "Java Developer", "Deploja AB", "Stockholm",
				"https://arbetsformedlingen.se/platsbanken/annonser/31696992");
	}

}
