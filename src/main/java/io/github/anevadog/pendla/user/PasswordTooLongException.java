package io.github.anevadog.pendla.user;

/**
 * Thrown when a password is longer than BCrypt accepts (72 bytes). Mapped to 400.
 */
public class PasswordTooLongException extends RuntimeException {

	public PasswordTooLongException() {
		super("Password is too long: the limit is 72 bytes, and accented characters or emojis take more than one");
	}

}
