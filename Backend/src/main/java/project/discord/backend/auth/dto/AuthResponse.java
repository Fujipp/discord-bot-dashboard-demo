package project.discord.backend.auth.dto;

public record AuthResponse(
        String message,
        UserResponse user
) {
}
