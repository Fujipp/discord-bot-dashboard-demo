package project.discord.backend.customer.dto;

import java.time.Instant;

public record BotConfigResponse(
        String key,
        String value,
        Boolean secret,
        String scope,
        Instant updatedAt
) {
}
