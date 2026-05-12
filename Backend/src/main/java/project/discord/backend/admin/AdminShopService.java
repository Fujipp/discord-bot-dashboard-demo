package project.discord.backend.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import project.discord.backend.admin.dto.AdminShopFeatureRequest;
import project.discord.backend.customer.domain.FeatureCatalogItem;
import project.discord.backend.customer.dto.FeatureResponse;
import project.discord.backend.customer.repository.FeatureCatalogRepository;

@Service
public class AdminShopService {

    private final FeatureCatalogRepository featureCatalogRepository;

    public AdminShopService(FeatureCatalogRepository featureCatalogRepository) {
        this.featureCatalogRepository = featureCatalogRepository;
    }

    public List<FeatureResponse> listFeatures() {
        return featureCatalogRepository.findAllByOrderBySortOrderAscMonthlyPriceCentsAsc().stream()
                .map(FeatureResponse::from)
                .toList();
    }

    public FeatureResponse updateFeature(Long featureId, AdminShopFeatureRequest request) {
        FeatureCatalogItem feature = featureCatalogRepository.findById(featureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature was not found"));

        feature.setName(normalizeText(request.name(), "Feature name", 100));
        feature.setDescription(normalizeText(request.description(), "Description", 500));
        feature.setMonthlyPriceCents(normalizePrice(request.monthlyPriceCents(), "Monthly price"));
        feature.setCurrency(normalizeCurrency(request.currency()));
        feature.setCategory(request.category() == null ? feature.getCategory() : request.category());
        feature.setPromotionLabel(normalizeOptionalText(request.promotionLabel(), 100));
        feature.setPromotionPriceCents(normalizeOptionalPrice(request.promotionPriceCents()));
        feature.setPromotionEndsAt(request.promotionEndsAt());
        feature.setFeatured(Boolean.TRUE.equals(request.featured()));
        feature.setSortOrder(request.sortOrder() == null ? 100 : Math.max(0, request.sortOrder()));
        feature.setActive(!Boolean.FALSE.equals(request.active()));

        return FeatureResponse.from(featureCatalogRepository.save(feature));
    }

    private String normalizeText(String value, String field, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }

        String trimmedValue = value.trim();
        if (trimmedValue.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be " + maxLength + " characters or fewer");
        }

        return trimmedValue;
    }

    private String normalizeOptionalText(String value, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String trimmedValue = value.trim();
        if (trimmedValue.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion label is too long");
        }

        return trimmedValue;
    }

    private Integer normalizePrice(Integer value, String field) {
        if (value == null || value < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be zero or greater");
        }

        return value;
    }

    private Integer normalizeOptionalPrice(Integer value) {
        if (value == null || value <= 0) {
            return null;
        }

        return value;
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "THB";
        }

        String normalizedCurrency = currency.trim().toUpperCase();
        if (normalizedCurrency.length() != 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency must be a 3-letter code");
        }

        return normalizedCurrency;
    }
}
