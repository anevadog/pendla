package io.github.anevadog.pendla.application;

/**
 * Status of a job application. Transition rules are added in phase 2.
 */
public enum ApplicationStatus {
	SAVED,
	SENT,
	INTERVIEW,
	REJECTED,
	OFFER
}
