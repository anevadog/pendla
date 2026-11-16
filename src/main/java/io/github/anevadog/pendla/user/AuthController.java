package io.github.anevadog.pendla.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.anevadog.pendla.user.dto.LoginRequest;
import io.github.anevadog.pendla.user.dto.RegisterRequest;
import io.github.anevadog.pendla.user.dto.TokenResponse;
import io.github.anevadog.pendla.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * HTTP layer for user accounts. No logic and no try/catch: errors go to GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Create a user account and log in")
// Empty on purpose: these endpoints are public, so Swagger shows them without the lock
@SecurityRequirements
public class AuthController {

	private final AuthService service;

	public AuthController(AuthService service) {
		this.service = service;
	}

	@Operation(summary = "Create a user account",
			description = "The username is stored in lowercase. 400 invalid data · 409 username already taken")
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse register(@Valid @RequestBody RegisterRequest request) {
		return service.register(request);
	}

	@Operation(summary = "Log in and get a token",
			description = "Returns a JWT valid for one hour. Paste it in the Authorize button to call the other endpoints. "
					+ "400 invalid data · 401 wrong username or password")
	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return service.login(request);
	}

}
