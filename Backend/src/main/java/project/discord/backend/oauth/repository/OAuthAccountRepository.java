package project.discord.backend.oauth.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import project.discord.backend.oauth.domain.OAuthAccount;
import project.discord.backend.oauth.domain.OAuthProvider;

public interface OAuthAccountRepository extends CrudRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderUserId(
            OAuthProvider provider,
            String providerUserId
    );

    Optional<OAuthAccount> findByUserIdAndProvider(Long userId, OAuthProvider provider);
}
