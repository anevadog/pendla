package io.github.anevadog.pendla.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Plain unit tests: no Spring and no mocks. Together the three tests cover all 25 status pairs.
 */
class ApplicationStatusTest {

	@ParameterizedTest(name = "{0} -> {1}")
	@CsvSource({
			"SAVED, SENT",
			"SENT, INTERVIEW",
			"SENT, REJECTED",
			"INTERVIEW, OFFER",
			"INTERVIEW, REJECTED"
	})
	void canTransitionTo_validTransition_returnsTrue(ApplicationStatus from, ApplicationStatus to) {
		assertThat(from.canTransitionTo(to)).isTrue();
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@CsvSource({
			"SAVED, SAVED",
			"SAVED, INTERVIEW",
			"SAVED, REJECTED",
			"SAVED, OFFER",
			"SENT, SAVED",
			"SENT, SENT",
			"SENT, OFFER",
			"INTERVIEW, SAVED",
			"INTERVIEW, SENT",
			"INTERVIEW, INTERVIEW"
	})
	void canTransitionTo_invalidTransition_returnsFalse(ApplicationStatus from, ApplicationStatus to) {
		assertThat(from.canTransitionTo(to)).isFalse();
	}

	@ParameterizedTest(name = "REJECTED/OFFER -> {0}")
	@EnumSource(ApplicationStatus.class)
	void canTransitionTo_fromFinalState_returnsFalse(ApplicationStatus target) {
		assertThat(ApplicationStatus.REJECTED.canTransitionTo(target)).isFalse();
		assertThat(ApplicationStatus.OFFER.canTransitionTo(target)).isFalse();
	}

}
