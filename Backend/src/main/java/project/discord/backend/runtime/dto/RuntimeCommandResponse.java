package project.discord.backend.runtime.dto;

import java.util.List;

public record RuntimeCommandResponse(
        String processName,
        String action,
        Boolean success,
        String output,
        List<RuntimeProcessResponse> processes
) {
}
