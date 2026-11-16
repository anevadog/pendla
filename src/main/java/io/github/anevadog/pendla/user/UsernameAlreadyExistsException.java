package io.github.anevadog.pendla.user;

/**
 * Thrown when someone registers a username that is already taken. Mapped to 409.
 */
public class UsernameAlreadyExistsException extends RuntimeException {

	public UsernameAlreadyExistsException(String username) {
		super("Username '" + username + "' is already taken");
	}

}
