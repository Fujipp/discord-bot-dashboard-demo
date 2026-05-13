package project.discord.backend.automation.dto;

import java.util.Map;

public record AutomationSettingUpdateRequest(
        Map<String, String> settings
) {
}
