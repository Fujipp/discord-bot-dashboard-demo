package project.discord.backend.runtime.dto;

public record RuntimeHostOptionResponse(
        String id,
        String name,
        String host,
        String region,
        Boolean primary
) {
}
