package project.discord.backend.admin.dto;

import project.discord.backend.customer.domain.BotBillingMode;
import project.discord.backend.customer.domain.BotStatus;
import project.discord.backend.customer.domain.DiscordBot;
import project.discord.backend.user.domain.UserAccount;

public record AdminRuntimeBotResponse(
        Long id,
        Long ownerUserId,
        String ownerEmail,
        String ownerUsername,
        String name,
        String pm2ProcessName,
        BotStatus status,
        BotBillingMode billingMode,
        Integer monthlyPriceCents
) {

    public static AdminRuntimeBotResponse from(DiscordBot bot, UserAccount owner) {
        return new AdminRuntimeBotResponse(
                bot.getId(),
                bot.getOwnerUserId(),
                owner == null ? null : owner.getEmail(),
                owner == null ? null : owner.getUsername(),
                bot.getName(),
                bot.getPm2ProcessName(),
                bot.getStatus(),
                bot.getBillingMode(),
                bot.getMonthlyPriceCents()
        );
    }
}
