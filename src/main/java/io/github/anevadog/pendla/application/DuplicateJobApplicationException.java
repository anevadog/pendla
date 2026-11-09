package io.github.anevadog.pendla.application;

/**
 * Thrown when an application for the same JobTech ad already exists. Mapped to 409.
 */
public class DuplicateJobApplicationException extends RuntimeException {

	public DuplicateJobApplicationException(String jobtechId) {
		super("An application for JobTech ad " + jobtechId + " already exists");
	}

}
