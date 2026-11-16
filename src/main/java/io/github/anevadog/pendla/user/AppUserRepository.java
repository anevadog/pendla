package io.github.anevadog.pendla.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	boolean existsByUsername(String username);

}
