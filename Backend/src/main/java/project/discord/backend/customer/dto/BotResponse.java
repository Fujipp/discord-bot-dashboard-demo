package project.discord.backend.customer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import project.discord.backend.customer.domain.BotStatus;
import project.discord.backend.customer.domain.DiscordBot;
import project.discord.backend.customer.domain.BotBillingMode;

public record BotResponse(
        Long id,
        String discordApplicationId,
        String pm2ProcessName,
        String name,
        String avatarUrl,
        BotStatus status,
        BotBillingMode billingMode,
        Integer monthlyPriceCents,
        Integer serverCount,
        Integer commandCount,
        BigDecimal uptimePercent,
        String hostedRegion,
        Instant lastHeartbeatAt,
        LocalDate runtimeExpiresAt,
        List<BotFeatureResponse> activeFeatures,
        List<BotConfigResponse> configEntries
) {

    public static BotResponse from(
            DiscordBot bot,
            List<BotFeatureResponse> activeFeatures,
            List<BotConfigResponse> configEntries
    ) {
        return new BotResponse(
                bot.getId(),
                bot.getDiscordApplicationId(),
                bot.getPm2ProcessName(),
                bot.getName(),
                bot.getAvatarUrl(),
                bot.getStatus(),
                bot.getBillingMode(),
                bot.getMonthlyPriceCents(),
                bot.getServerCount(),
                bot.getCommandCount(),
                bot.getUptimePercent(),
                bot.getHostedRegion(),
                bot.getLastHeartbeatAt(),
                activeFeatures.stream()
                        .filter(feature -> "runtime-247".equals(feature.code()))
                        .findFirst()
                        .map(BotFeatureResponse::currentPeriodEnd)
                        .orElse(null),
                activeFeatures,
                configEntries
        );
    }
}
