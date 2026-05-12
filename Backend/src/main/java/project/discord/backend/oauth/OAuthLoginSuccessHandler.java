package project.discord.backend.oauth;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import project.discord.backend.auth.TokenService;
import project.discord.backend.oauth.domain.OAuthProvider;
import project.discord.backend.oauth.repository.OAuthAccountRepository;
import project.discord.backend.user.domain.UserAccount;
import project.discord.backend.user.repository.UserRepository;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final String frontendUrl;
    private final OAuthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public OAuthLoginSuccessHandler(
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl,
            OAuthAccountRepository oauthAccountRepository,
            UserRepository userRepository,
            TokenService tokenService
    ) {
        this.frontendUrl = frontendUrl;
        this.oauthAccountRepository = oauthAccountRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = oauthToken.getPrincipal();
        String provider = oauthToken.getAuthorizedClientRegistrationId();
        Map<String, Object> attributes = principal.getAttributes();
        UserAccount user = findUser(provider, attributes);

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/callback")
                .queryParam("status", "success")
                .queryParam("provider", provider.toLowerCase(Locale.ROOT))
                .queryParam("token", tokenService.createAccessToken(user.getId()))
                .queryParam("id", user.getId())
                .queryParam("email", user.getEmail())
                .queryParam("username", user.getUsername())
                .queryParam("age", user.getAge())
                .queryParam("avatarUrl", user.getAvatarUrl())
                .queryParam("role", user.getRole())
                .queryParam("userStatus", user.getStatus())
                .queryParam("emailVerified", user.getEmailVerified())
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private UserAccount findUser(String provider, Map<String, Object> attributes) {
        Object providerUserId = attributes.get("id");
        if (providerUserId == null) {
            providerUserId = attributes.get("sub");
        }

        return oauthAccountRepository
                .findByProviderAndProviderUserId(
                        OAuthProvider.valueOf(provider.toUpperCase(Locale.ROOT)),
                        String.valueOf(providerUserId)
                )
                .flatMap(account -> userRepository.findById(account.getUserId()))
                .orElseThrow();
    }
}
