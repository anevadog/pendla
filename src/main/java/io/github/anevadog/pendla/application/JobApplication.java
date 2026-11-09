package io.github.anevadog.pendla.application;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A job ad I am interested in, and the status of my application to it.
 * Never returned directly in an HTTP response: see JobApplicationResponse.
 */
@Entity
@Table(name = "job_application")
public class JobApplication {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Optional: null when the application does not come from JobTech.
	// Unique as a safety net; the service checks duplicates first (F1-2).
	@Column(name = "jobtech_id", unique = true, length = 50)
	private String jobtechId;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String employer;

	private String city;

	@Column(length = 2048)
	private String url;

	// STRING stores "SAVED" instead of 0, so reordering the enum never corrupts data
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ApplicationStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** Required by JPA. Protected so the rest of the code uses the constructor below. */
	protected JobApplication() {
	}

	public JobApplication(String jobtechId, String title, String employer, String city, String url) {
		this.jobtechId = jobtechId;
		this.title = title;
		this.employer = employer;
		this.city = city;
		this.url = url;
		this.status = ApplicationStatus.SAVED;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getJobtechId() {
		return jobtechId;
	}

	public String getTitle() {
		return title;
	}

	public String getEmployer() {
		return employer;
	}

	public String getCity() {
		return city;
	}

	public String getUrl() {
		return url;
	}

	public ApplicationStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
