package io.github.anevadog.pendla.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.github.anevadog.pendla.user.dto.RegisterRequest;
import io.github.anevadog.pendla.user.dto.UserResponse;

/**
 * Unit tests: the repository and the password encoder are Mockito mocks, no Spring context and no database.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private AppUserRepository repository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private AuthService service;

	@Test
	void register_withNewUsername_savesLowercaseUsernameAndHashedPassword() {
		when(repository.existsByUsername("alex")).thenReturn(false);
		when(passwordEncoder.encode("secret-password")).thenReturn("hashed-password");
		// saveAndFlush() returns the same entity it receives
		when(repository.saveAndFlush(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = service.register(new RegisterRequest("Alex", "secret-password"));

		assertThat(response.username()).isEqualTo("alex");
		ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
		verify(repository).saveAndFlush(saved.capture());
		assertThat(saved.getValue().getUsername()).isEqualTo("alex");
		// What is stored is the hash, never the password
		assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed-password");
		assertThat(saved.getValue().getCreatedAt()).isNotNull();
	}

	@Test
	void register_withExistingUsername_throwsUsernameAlreadyExists() {
		// The check uses the lowercase username, so "ALEX" collides with "alex"
		when(repository.existsByUsername("alex")).thenReturn(true);

		assertThatThrownBy(() -> service.register(new RegisterRequest("ALEX", "secret-password")))
				.isInstanceOf(UsernameAlreadyExistsException.class)
				.hasMessageContaining("alex");
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void register_whenUniqueConstraintFails_throwsUsernameAlreadyExists() {
		// Race: the check finds nothing, but another request inserts the same username before ours
		when(repository.existsByUsername("alex")).thenReturn(false);
		when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
		when(repository.saveAndFlush(any(AppUser.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate key"));

		assertThatThrownBy(() -> service.register(new RegisterRequest("alex", "secret-password")))
				.isInstanceOf(UsernameAlreadyExistsException.class);
	}

	@Test
	void register_withPasswordOver72Bytes_throwsPasswordTooLong() {
		// 40 characters, so it passes @Size(max = 72), but "ñ" is 2 bytes in UTF-8: 80 bytes
		String password = "ñ".repeat(40);

		assertThatThrownBy(() -> service.register(new RegisterRequest("alex", password)))
				.isInstanceOf(PasswordTooLongException.class);
		verify(passwordEncoder, never()).encode(anyString());
		verify(repository, never()).saveAndFlush(any());
	}

}
