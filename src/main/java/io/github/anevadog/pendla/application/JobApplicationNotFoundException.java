package io.github.anevadog.pendla.application;

/**
 * Thrown when no application exists with the given id. Mapped to 404.
 */
public class JobApplicationNotFoundException extends RuntimeException {

	public JobApplicationNotFoundException(Long id) {
		super("Job application " + id + " not found");
	}

}
