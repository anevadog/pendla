package io.github.anevadog.pendla.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.github.anevadog.pendla.user.AppUser;

/**
 * Plain unit tests of the entity's own behaviour: no Spring and no database.
 */
class JobApplicationTest {

	private JobApplication newApplication() {
		return new JobApplication(new AppUser("alex", "hash"), null, "Java Developer", "Deploja AB", null, null);
	}

	@Test
	void changeStatus_validTransition_updatesStatus() {
		JobApplication application = newApplication();

		application.changeStatus(ApplicationStatus.SENT);

		assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SENT);
	}

	@Test
	void changeStatus_invalidTransition_throwsAndKeepsStatus() {
		JobApplication application = newApplication();

		assertThatThrownBy(() -> application.changeStatus(ApplicationStatus.OFFER))
				.isInstanceOf(InvalidStatusTransitionException.class)
				.hasMessage("Cannot change status from SAVED to OFFER");
		assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SAVED);
	}

	@Test
	void updateDetails_withValidData_updatesBothFields() {
		JobApplication application = newApplication();
		Instant interviewAt = Instant.parse("2026-11-20T09:00:00Z");

		application.updateDetails("Prepare Spring questions", interviewAt);

		assertThat(application.getNotes()).isEqualTo("Prepare Spring questions");
		assertThat(application.getInterviewAt()).isEqualTo(interviewAt);
	}

	@Test
	void updateDetails_withNulls_clearsBothFields() {
		JobApplication application = newApplication();
		application.updateDetails("Old notes", Instant.parse("2026-11-20T09:00:00Z"));

		application.updateDetails(null, null);

		assertThat(application.getNotes()).isNull();
		assertThat(application.getInterviewAt()).isNull();
	}

}
