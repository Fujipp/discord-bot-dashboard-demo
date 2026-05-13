package project.discord.backend.customer.dto;

import java.util.Map;

public record BotConfigUpdateRequest(
        Map<String, String> values
) {
}
