package project.discord.backend.customer.dto;

import project.discord.backend.customer.domain.FeatureCatalogItem;
import project.discord.backend.customer.domain.FeatureCategory;

public record FeatureResponse(
        Long id,
        String code,
        String name,
        String description,
        Integer monthlyPriceCents,
        String currency,
        FeatureCategory category
) {

    public static FeatureResponse from(FeatureCatalogItem feature) {
        return new FeatureResponse(
                feature.getId(),
                feature.getCode(),
                feature.getName(),
                feature.getDescription(),
                feature.getMonthlyPriceCents(),
                feature.getCurrency(),
                feature.getCategory()
        );
    }
}
