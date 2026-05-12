package project.discord.backend.customer.dto;

import java.time.Instant;

import project.discord.backend.customer.domain.FeatureCatalogItem;
import project.discord.backend.customer.domain.FeatureCategory;

public record FeatureResponse(
        Long id,
        String code,
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

    public static FeatureResponse from(FeatureCatalogItem feature) {
        return new FeatureResponse(
                feature.getId(),
                feature.getCode(),
                feature.getName(),
                feature.getDescription(),
                feature.getMonthlyPriceCents(),
                feature.getCurrency(),
                feature.getCategory(),
                feature.getPromotionLabel(),
                feature.getPromotionPriceCents(),
                feature.getPromotionEndsAt(),
                feature.getFeatured(),
                feature.getSortOrder(),
                feature.getActive()
        );
    }
}
