package io.github.anevadog.pendla.jobsearch;

/**
 * JobTech answered with an error, an unreadable body, or could not be reached. Mapped to 502.
 */
public class JobTechUnavailableException extends RuntimeException {

	public JobTechUnavailableException(Throwable cause) {
		super("JobTech is not available right now, try again later", cause);
	}

}
