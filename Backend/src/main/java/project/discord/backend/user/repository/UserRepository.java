package project.discord.backend.user.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jdbc.repository.query.Query;

import project.discord.backend.user.domain.UserAccount;

public interface UserRepository extends CrudRepository<UserAccount, Long> {

    List<UserAccount> findAllByOrderByCreatedAtDesc();

    @Query("""
            SELECT *
            FROM users
            WHERE LOWER(email) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(username) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY created_at DESC
            LIMIT :limit
            """)
    List<UserAccount> searchByEmailOrUsername(String query, Integer limit);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
