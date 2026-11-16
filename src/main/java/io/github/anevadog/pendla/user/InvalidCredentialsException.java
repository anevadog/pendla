package io.github.anevadog.pendla.user;

/**
 * Thrown when a login fails. Mapped to 401.
 * The message is the same for an unknown user and for a wrong password, so it does not reveal which users exist.
 */
public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Invalid username or password");
	}

}
