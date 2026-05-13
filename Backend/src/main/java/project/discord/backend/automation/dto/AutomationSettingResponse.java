package project.discord.backend.automation.dto;

public record AutomationSettingResponse(
        String key,
        String value,
        String description
) {
}
