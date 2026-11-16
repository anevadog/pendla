package io.github.anevadog.pendla.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A user of the API. Never returned in an HTTP response: see UserResponse.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Always stored in lowercase, so "Alex" and "alex" are the same user
	@Column(nullable = false, unique = true, length = 30)
	private String username;

	// BCrypt hash of the password. The password itself is never stored
	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** Required by JPA. Protected so the rest of the code uses the constructor below. */
	protected AppUser() {
	}

	public AppUser(String username, String passwordHash) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
