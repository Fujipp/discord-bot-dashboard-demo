package project.discord.backend.admin.dto;

import java.time.LocalDate;

import project.discord.backend.customer.domain.BotBillingMode;

public record AdminProcessAssignmentRequest(
        Long ownerUserId,
        String botName,
        BotBillingMode billingMode,
        Integer monthlyPriceCents,
        LocalDate runtimeCurrentPeriodEnd
) {
}
