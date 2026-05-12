package project.discord.backend.runtime.dto;

public record RuntimeProcessResponse(
        String name,
        String status,
        Integer pid,
        Double cpu,
        Long memoryBytes,
        Integer restartCount,
        Long uptimeMillis
) {
}
