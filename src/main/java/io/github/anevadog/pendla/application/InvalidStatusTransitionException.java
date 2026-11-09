package io.github.anevadog.pendla.application;

/**
 * Thrown when an application cannot move from its current status to the requested one. Mapped to 409.
 */
public class InvalidStatusTransitionException extends RuntimeException {

	public InvalidStatusTransitionException(ApplicationStatus from, ApplicationStatus to) {
		super("Cannot change status from " + from + " to " + to);
	}

}
