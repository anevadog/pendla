package io.github.anevadog.pendla.user;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.anevadog.pendla.user.dto.LoginRequest;
import io.github.anevadog.pendla.user.dto.RegisterRequest;
import io.github.anevadog.pendla.user.dto.TokenResponse;
import io.github.anevadog.pendla.user.dto.UserResponse;

/**
 * Business logic for user accounts. Receives and returns DTOs; entities stay inside.
 */
@Service
public class AuthService {

	// BCrypt only uses the first 72 bytes of a password
	private static final int MAX_PASSWORD_BYTES = 72;

	private final AppUserRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;

	public AuthService(AppUserRepository repository, PasswordEncoder passwordEncoder, TokenService tokenService) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
	}

	/**
	 * Checks the password and returns a token. An unknown user and a wrong password
	 * give the same error, so the answer does not reveal which users exist.
	 */
	@Transactional(readOnly = true)
	public TokenResponse login(LoginRequest request) {
		// Same conversion as in register: usernames are stored in lowercase
		String username = request.username().toLowerCase(Locale.ROOT);

		AppUser user = repository.findByUsername(username)
				.orElseThrow(InvalidCredentialsException::new);

		// matches() hashes the password with the salt stored inside the hash and compares the results
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}

		return tokenService.createToken(user.getUsername());
	}

	@Transactional
	public UserResponse register(RegisterRequest request) {
		// Locale.ROOT: the result does not depend on the language of the machine
		String username = request.username().toLowerCase(Locale.ROOT);

		// @Size counts characters, but the BCrypt limit is in bytes ("ñ" is 2 bytes in UTF-8)
		if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
			throw new PasswordTooLongException();
		}

		if (repository.existsByUsername(username)) {
			throw new UsernameAlreadyExistsException(username);
		}

		AppUser user = new AppUser(username, passwordEncoder.encode(request.password()));

		try {
			// saveAndFlush runs the INSERT now, so a database error surfaces here and not at commit
			return UserResponse.from(repository.saveAndFlush(user));
		} catch (DataIntegrityViolationException ex) {
			// Two registrations with the same username at the same time: both pass the check above,
			// and the unique constraint rejects the second INSERT. Same answer as the check: 409.
			throw new UsernameAlreadyExistsException(username);
		}
	}

}
