package io.github.anevadog.pendla.user;

/**
 * Thrown when someone tries to register and registration is switched off (registration.enabled=false).
 * Mapped to 403: the request is understood and well formed, but this server does not allow it.
 */
public class RegistrationDisabledException extends RuntimeException {

	public RegistrationDisabledException() {
		super("Registration is closed on this server");
	}

}
