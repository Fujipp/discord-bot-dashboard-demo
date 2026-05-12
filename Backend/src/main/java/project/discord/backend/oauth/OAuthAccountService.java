package project.discord.backend.oauth;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import project.discord.backend.oauth.domain.OAuthAccount;
import project.discord.backend.oauth.domain.OAuthProvider;
import project.discord.backend.oauth.repository.OAuthAccountRepository;
import project.discord.backend.user.domain.UserAccount;
import project.discord.backend.user.domain.UserRole;
import project.discord.backend.user.domain.UserStatus;
import project.discord.backend.user.repository.UserRepository;

@Service
public class OAuthAccountService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;

    public OAuthAccountService(UserRepository userRepository, OAuthAccountRepository oauthAccountRepository) {
        this.userRepository = userRepository;
        this.oauthAccountRepository = oauthAccountRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = delegate.loadUser(userRequest);
        OAuthProvider provider = OAuthProvider.valueOf(
                userRequest.getClientRegistration().getRegistrationId().toUpperCase(Locale.ROOT)
        );
        Map<String, Object> attributes = oauthUser.getAttributes();

        String providerUserId = requireProviderUserId(attributes);
        String email = normalizeEmail(extractEmail(attributes), provider, providerUserId);
        String username = extractUsername(provider, attributes, email);
        String avatarUrl = extractAvatarUrl(provider, attributes);

        UserAccount user = oauthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .flatMap(account -> userRepository.findById(account.getUserId()))
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> createUser(email, username, avatarUrl));

        if (user.getAvatarUrl() == null && avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
            user.setUpdatedAt(Instant.now());
            user = userRepository.save(user);
        }

        linkOAuthAccount(user, provider, providerUserId, username, email);
        return oauthUser;
    }

    private UserAccount createUser(String email, String username, String avatarUrl) {
        Instant now = Instant.now();
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setUsername(generateUniqueUsername(username));
        user.setAge(null);
        user.setAvatarUrl(avatarUrl);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    private void linkOAuthAccount(
            UserAccount user,
            OAuthProvider provider,
            String providerUserId,
            String providerUsername,
            String providerEmail
    ) {
        OAuthAccount account = oauthAccountRepository.findByUserIdAndProvider(user.getId(), provider)
                .orElseGet(OAuthAccount::new);
        Instant now = Instant.now();

        account.setUserId(user.getId());
        account.setProvider(provider);
        account.setProviderUserId(providerUserId);
        account.setProviderUsername(providerUsername);
        account.setProviderEmail(providerEmail);
        if (account.getCreatedAt() == null) {
            account.setCreatedAt(now);
        }
        account.setUpdatedAt(now);

        oauthAccountRepository.save(account);
    }

    private String requireProviderUserId(Map<String, Object> attributes) {
        Object id = attributes.get("id");
        if (id == null) {
            id = attributes.get("sub");
        }
        if (id == null) {
            throw new OAuth2AuthenticationException("OAuth provider did not return a user id");
        }
        return String.valueOf(id);
    }

    private String extractEmail(Map<String, Object> attributes) {
        Object email = attributes.get("email");
        return email == null || String.valueOf(email).isBlank() ? null : String.valueOf(email);
    }

    private String normalizeEmail(String email, OAuthProvider provider, String providerUserId) {
        if (email != null && !email.isBlank()) {
            return email.trim().toLowerCase(Locale.ROOT);
        }
        return provider.name().toLowerCase(Locale.ROOT) + "-" + providerUserId + "@oauth.local";
    }

    private String extractUsername(OAuthProvider provider, Map<String, Object> attributes, String email) {
        Object username = switch (provider) {
            case DISCORD -> attributes.get("username");
            case GITHUB -> attributes.get("login");
            case GOOGLE -> attributes.get("name");
        };

        if (username != null && !String.valueOf(username).isBlank()) {
            return String.valueOf(username);
        }
        return email.substring(0, email.indexOf('@'));
    }

    private String extractAvatarUrl(OAuthProvider provider, Map<String, Object> attributes) {
        Object avatar = switch (provider) {
            case DISCORD -> buildDiscordAvatarUrl(attributes);
            case GITHUB -> attributes.get("avatar_url");
            case GOOGLE -> attributes.get("picture");
        };

        return avatar == null || String.valueOf(avatar).isBlank() ? null : String.valueOf(avatar);
    }

    private String buildDiscordAvatarUrl(Map<String, Object> attributes) {
        Object id = attributes.get("id");
        Object avatar = attributes.get("avatar");
        if (id == null || avatar == null) {
            return null;
        }
        return "https://cdn.discordapp.com/avatars/" + id + "/" + avatar + ".png";
    }

    private String generateUniqueUsername(String username) {
        String baseUsername = username
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");

        if (baseUsername.length() < 3) {
            baseUsername = "user_" + baseUsername;
        }
        if (baseUsername.length() > 24) {
            baseUsername = baseUsername.substring(0, 24);
        }

        String candidate = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            String suffixText = "_" + suffix++;
            int maxBaseLength = 32 - suffixText.length();
            candidate = baseUsername.substring(0, Math.min(baseUsername.length(), maxBaseLength)) + suffixText;
        }

        return candidate;
    }
}
