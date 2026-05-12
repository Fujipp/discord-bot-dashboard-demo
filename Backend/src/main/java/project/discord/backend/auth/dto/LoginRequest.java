package project.discord.backend.auth.dto;

public record LoginRequest(
        String email,
        String password
) {
}
