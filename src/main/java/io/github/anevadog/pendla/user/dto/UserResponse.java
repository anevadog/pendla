package io.github.anevadog.pendla.user.dto;

import io.github.anevadog.pendla.user.AppUser;

/**
 * What the API returns about a user. It has no password and no hash on purpose.
 */
public record UserResponse(Long id, String username) {

	public static UserResponse from(AppUser user) {
		return new UserResponse(user.getId(), user.getUsername());
	}

}
