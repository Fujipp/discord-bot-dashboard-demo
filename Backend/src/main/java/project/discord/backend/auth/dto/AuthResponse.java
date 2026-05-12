package project.discord.backend.auth.dto;

public record AuthResponse(
        String message,
        String accessToken,
        UserResponse user
) {
}
