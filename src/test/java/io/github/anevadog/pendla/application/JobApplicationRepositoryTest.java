package io.github.anevadog.pendla.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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

import io.github.anevadog.pendla.user.AppUser;
import io.github.anevadog.pendla.user.AppUserRepository;
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

	@Autowired
	private AppUserRepository userRepository;

	private AppUser alex;
	private AppUser maria;

	// An application needs an owner that already exists in app_user (foreign key)
	@BeforeEach
	void saveUsers() {
		alex = userRepository.saveAndFlush(new AppUser("alex", "hash"));
		maria = userRepository.saveAndFlush(new AppUser("maria", "hash"));
	}

	@Test
	void save_withAllFields_readsBackTheSameValues() {
		Instant interviewAt = Instant.parse("2026-11-20T09:00:00Z");
		JobApplication application = newApplication(alex, "31696992");
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
	void save_withSameJobtechIdAndSameOwner_throwsDataIntegrityViolation() {
		repository.saveAndFlush(newApplication(alex, "31696992"));

		assertThatThrownBy(() -> repository.saveAndFlush(newApplication(alex, "31696992")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void save_withSameJobtechIdAndDifferentOwners_savesBoth() {
		repository.saveAndFlush(newApplication(alex, "31696992"));
		repository.saveAndFlush(newApplication(maria, "31696992"));

		assertThat(repository.count()).isEqualTo(2);
	}

	@Test
	void save_withTwoNullJobtechIds_savesBoth() {
		repository.saveAndFlush(newApplication(alex, null));
		repository.saveAndFlush(newApplication(alex, null));

		assertThat(repository.count()).isEqualTo(2);
	}

	@Test
	void existsByOwnerAndJobtechId_onlyTrueForTheOwnerWhoSavedIt() {
		repository.saveAndFlush(newApplication(alex, "31696992"));

		assertThat(repository.existsByOwnerAndJobtechId(alex, "31696992")).isTrue();
		assertThat(repository.existsByOwnerAndJobtechId(alex, "00000000")).isFalse();
		assertThat(repository.existsByOwnerAndJobtechId(maria, "31696992")).isFalse();
	}

	@Test
	void findByIdAndOwner_withAnotherUsersApplication_returnsEmpty() {
		Long id = repository.saveAndFlush(newApplication(alex, "31696992")).getId();

		assertThat(repository.findByIdAndOwner(id, alex)).isPresent();
		assertThat(repository.findByIdAndOwner(id, maria)).isEmpty();
	}

	@Test
	void findAllByOwnerOrderByCreatedAtDesc_returnsOnlyThatOwnersApplications() {
		repository.saveAndFlush(newApplication(alex, "1"));
		repository.saveAndFlush(newApplication(maria, "2"));

		List<JobApplication> found = repository.findAllByOwnerOrderByCreatedAtDesc(alex);

		assertThat(found).extracting(JobApplication::getJobtechId).containsExactly("1");
	}

	@Test
	void findAllByOwnerAndStatusOrderByCreatedAtDesc_withSent_returnsOnlyOwnSentApplications() {
		JobApplication sent = newApplication(alex, "1");
		sent.changeStatus(ApplicationStatus.SENT);
		repository.saveAndFlush(sent);
		repository.saveAndFlush(newApplication(alex, "2"));
		JobApplication sentByMaria = newApplication(maria, "3");
		sentByMaria.changeStatus(ApplicationStatus.SENT);
		repository.saveAndFlush(sentByMaria);

		List<JobApplication> found = repository.findAllByOwnerAndStatusOrderByCreatedAtDesc(alex,
				ApplicationStatus.SENT);

		assertThat(found).extracting(JobApplication::getJobtechId).containsExactly("1");
	}

	private JobApplication newApplication(AppUser owner, String jobtechId) {
		return new JobApplication(owner, jobtechId, "Java Developer", "Deploja AB", "Stockholm",
				"https://arbetsformedlingen.se/platsbanken/annonser/31696992");
	}

}
