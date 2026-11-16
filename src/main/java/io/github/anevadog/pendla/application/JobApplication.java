package io.github.anevadog.pendla.application;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import io.github.anevadog.pendla.user.AppUser;

/**
 * A job ad a user is interested in, and the status of their application to it.
 * Never returned directly in an HTTP response: see JobApplicationResponse.
 */
@Entity
@Table(name = "job_application")
public class JobApplication {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Many applications belong to one user; the column user_id is the foreign key.
	// LAZY: the user row is not loaded with the application, only if the code asks for it.
	// updatable = false: an application never changes owner.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, updatable = false)
	private AppUser owner;

	// Optional: null when the application does not come from JobTech.
	// Unique per owner in the database as a safety net; the service checks duplicates first (F1-2).
	@Column(name = "jobtech_id", length = 50)
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

	// Free text. It must not contain third-party contact data (warned in Swagger and README)
	@Column(length = 2000)
	private String notes;

	// Stored in UTC: PostgreSQL TIMESTAMPTZ does not keep the original offset
	@Column(name = "interview_at")
	private Instant interviewAt;

	/** Required by JPA. Protected so the rest of the code uses the constructor below. */
	protected JobApplication() {
	}

	public JobApplication(AppUser owner, String jobtechId, String title, String employer, String city, String url) {
		this.owner = owner;
		this.jobtechId = jobtechId;
		this.title = title;
		this.employer = employer;
		this.city = city;
		this.url = url;
		this.status = ApplicationStatus.SAVED;
		this.createdAt = Instant.now();
	}

	/**
	 * The only way to change the status: the transition rules live in ApplicationStatus.
	 *
	 * @throws InvalidStatusTransitionException if the current status cannot move to newStatus
	 */
	public void changeStatus(ApplicationStatus newStatus) {
		if (!status.canTransitionTo(newStatus)) {
			throw new InvalidStatusTransitionException(status, newStatus);
		}
		this.status = newStatus;
	}

	/**
	 * Replaces both details at once (PUT semantics): a null value clears that field.
	 * Independent of the status: an interview date can be set before or after moving to INTERVIEW.
	 */
	public void updateDetails(String notes, Instant interviewAt) {
		this.notes = notes;
		this.interviewAt = interviewAt;
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

	public String getNotes() {
		return notes;
	}

	public Instant getInterviewAt() {
		return interviewAt;
	}

}
