package project.discord.backend.auth.dto;

public record RegisterRequest(
        String email,
        String username,
        String password,
        Integer age
) {
}
