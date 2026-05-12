package project.discord.backend.customer.dto;

import java.time.LocalDate;

import project.discord.backend.customer.domain.BotFeatureSubscription;
import project.discord.backend.customer.domain.FeatureCatalogItem;
import project.discord.backend.customer.domain.SubscriptionStatus;

public record BotFeatureResponse(
        Long subscriptionId,
        Long featureId,
        String code,
        String name,
        SubscriptionStatus status,
        LocalDate currentPeriodEnd,
        Boolean autoRenew
) {

    public static BotFeatureResponse from(BotFeatureSubscription subscription, FeatureCatalogItem feature) {
        return new BotFeatureResponse(
                subscription.getId(),
                feature.getId(),
                feature.getCode(),
                feature.getName(),
                subscription.getStatus(),
                subscription.getCurrentPeriodEnd(),
                subscription.getAutoRenew()
        );
    }
}
