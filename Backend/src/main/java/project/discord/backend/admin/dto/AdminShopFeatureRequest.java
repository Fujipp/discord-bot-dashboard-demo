package project.discord.backend.admin.dto;

import java.time.Instant;

import project.discord.backend.customer.domain.FeatureCategory;

public record AdminShopFeatureRequest(
        String name,
        String description,
        Integer monthlyPriceCents,
        String currency,
        FeatureCategory category,
        String promotionLabel,
        Integer promotionPriceCents,
        Instant promotionEndsAt,
        Boolean featured,
        Integer sortOrder,
        Boolean active
) {
}
