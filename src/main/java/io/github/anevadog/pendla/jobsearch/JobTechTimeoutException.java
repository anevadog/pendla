package io.github.anevadog.pendla.jobsearch;

/**
 * JobTech did not answer within the configured timeout. Mapped to 504.
 */
public class JobTechTimeoutException extends RuntimeException {

	public JobTechTimeoutException(Throwable cause) {
		super("JobTech took too long to answer, try again later", cause);
	}

}
