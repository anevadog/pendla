package io.github.anevadog.pendla.application;

/**
 * Status of a job application and the transitions allowed between statuses.
 *
 * SAVED → SENT → INTERVIEW → OFFER, and SENT or INTERVIEW → REJECTED.
 * REJECTED and OFFER are final: no transition leaves them.
 */
public enum ApplicationStatus {
	SAVED,
	SENT,
	INTERVIEW,
	REJECTED,
	OFFER;

	/**
	 * Whether an application in this status can move to the target status.
	 * Staying in the same status is not a transition, so it returns false.
	 *
	 * The switch has no default branch on purpose: if a new status is added,
	 * the code does not compile until its transitions are decided here.
	 */
	public boolean canTransitionTo(ApplicationStatus target) {
		return switch (this) {
			case SAVED -> target == SENT;
			case SENT -> target == INTERVIEW || target == REJECTED;
			case INTERVIEW -> target == OFFER || target == REJECTED;
			case REJECTED, OFFER -> false;
		};
	}
}
