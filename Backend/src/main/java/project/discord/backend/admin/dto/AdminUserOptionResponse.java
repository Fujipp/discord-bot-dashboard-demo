package project.discord.backend.admin.dto;

import project.discord.backend.user.domain.UserAccount;
import project.discord.backend.user.domain.UserRole;

public record AdminUserOptionResponse(
        Long id,
        String email,
        String username,
        UserRole role
) {

    public static AdminUserOptionResponse from(UserAccount user) {
        return new AdminUserOptionResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole()
        );
    }
}
