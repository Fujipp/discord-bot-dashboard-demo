package project.discord.backend.auth.dto;

import java.time.Instant;

import project.discord.backend.user.domain.UserAccount;
import project.discord.backend.user.domain.UserRole;
import project.discord.backend.user.domain.UserStatus;

public record UserResponse(
        Long id,
        String email,
        String username,
        Integer age,
        String avatarUrl,
        UserRole role,
        UserStatus status,
        Boolean emailVerified,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getAge(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getStatus(),
                user.getEmailVerified(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
