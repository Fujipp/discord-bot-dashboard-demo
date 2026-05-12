package project.discord.backend.customer.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import project.discord.backend.customer.domain.FeatureCatalogItem;

public interface FeatureCatalogRepository extends CrudRepository<FeatureCatalogItem, Long> {

    List<FeatureCatalogItem> findByActiveTrueOrderBySortOrderAscMonthlyPriceCentsAsc();

    List<FeatureCatalogItem> findAllByOrderBySortOrderAscMonthlyPriceCentsAsc();
}
