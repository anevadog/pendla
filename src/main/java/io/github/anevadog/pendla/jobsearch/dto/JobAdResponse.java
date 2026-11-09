package io.github.anevadog.pendla.jobsearch.dto;

/**
 * Simplified job ad returned by GET /api/jobs. No contact data.
 */
public record JobAdResponse(
		String jobtechId,
		String title,
		String employer,
		String city,
		String url) {

	/** JobTech may omit employer or workplace_address: those fields become null instead of failing. */
	public static JobAdResponse from(JobTechSearchResponse.Hit hit) {
		String employer = hit.employer() == null ? null : hit.employer().name();
		String city = hit.workplaceAddress() == null ? null : hit.workplaceAddress().municipality();
		return new JobAdResponse(hit.id(), hit.headline(), employer, city, hit.webpageUrl());
	}

}
