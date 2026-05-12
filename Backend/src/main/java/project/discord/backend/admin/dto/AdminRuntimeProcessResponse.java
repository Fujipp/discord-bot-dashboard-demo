package project.discord.backend.admin.dto;

import project.discord.backend.runtime.dto.RuntimeProcessResponse;

public record AdminRuntimeProcessResponse(
        RuntimeProcessResponse runtime,
        AdminRuntimeBotResponse bot
) {
}
