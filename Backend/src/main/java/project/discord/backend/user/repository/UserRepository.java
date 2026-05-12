package project.discord.backend.user.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import project.discord.backend.user.domain.UserAccount;

public interface UserRepository extends CrudRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
