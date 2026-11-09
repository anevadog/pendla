package io.github.anevadog.pendla.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Plain unit tests of the entity's own behaviour: no Spring and no database.
 */
class JobApplicationTest {

	private JobApplication newApplication() {
		return new JobApplication(null, "Java Developer", "Deploja AB", null, null);
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

}
