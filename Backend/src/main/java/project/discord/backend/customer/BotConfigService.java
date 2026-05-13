package project.discord.backend.customer;

import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import project.discord.backend.customer.dto.BotConfigUpdateRequest;
import project.discord.backend.user.domain.UserAccount;

@Service
public class BotConfigService {

    private static final Pattern SAFE_KEY = Pattern.compile("^[a-zA-Z0-9._:-]{2,120}$");

    private final JdbcClient jdbcClient;

    public BotConfigService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional
    public void updateBotConfig(UserAccount user, Long botId, BotConfigUpdateRequest request) {
        requireOwnedBot(user, botId);
        if (request == null || request.values() == null) {
            return;
        }

        for (Map.Entry<String, String> entry : request.values().entrySet()) {
            String key = normalizeKey(entry.getKey());
            String value = normalizeValue(entry.getValue());
            boolean secret = isSecretKey(key);

            jdbcClient.sql("""
                    INSERT INTO bot_config_entries (bot_id, config_key, config_value, is_secret, scope)
                    VALUES (:botId, :configKey, :configValue, :secret, :scope)
                    ON DUPLICATE KEY UPDATE
                      config_value = VALUES(config_value),
                      is_secret = VALUES(is_secret),
                      scope = VALUES(scope)
                    """)
                    .param("botId", botId)
                    .param("configKey", key)
                    .param("configValue", value)
                    .param("secret", secret)
                    .param("scope", scopeFor(key))
                    .update();
        }
    }

    private void requireOwnedBot(UserAccount user, Long botId) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login is required");
        }

        boolean exists = jdbcClient.sql("""
                SELECT COUNT(*)
                FROM discord_bots
                WHERE id = :botId
                  AND owner_user_id = :userId
                """)
                .param("botId", botId)
                .param("userId", user.getId())
                .query(Integer.class)
                .single() > 0;

        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bot was not found");
        }
    }

    private String normalizeKey(String key) {
        String normalized = key == null ? "" : key.trim();
        if (!SAFE_KEY.matcher(normalized).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid config key");
        }
        return normalized;
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() > 4000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Config value is too long");
        }
        return value;
    }

    private boolean isSecretKey(String key) {
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("token")
                || lowerKey.contains("secret")
                || lowerKey.contains("password")
                || lowerKey.contains("api_key")
                || lowerKey.contains("apikey")
                || lowerKey.endsWith("_key");
    }

    private String scopeFor(String key) {
        int separator = key.indexOf('.');
        return separator < 1 ? "bot" : key.substring(0, separator);
    }
}
