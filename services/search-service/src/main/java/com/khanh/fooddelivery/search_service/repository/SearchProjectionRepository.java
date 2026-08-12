package com.khanh.fooddelivery.search_service.repository;

import com.khanh.fooddelivery.search_service.document.BranchSearchProjection;
import com.khanh.fooddelivery.search_service.document.CatalogItemSearchProjection;
import com.khanh.fooddelivery.search_service.dto.ItemSearchCriteria;
import com.fasterxml.jackson.databind.JsonNode;

public interface SearchProjectionRepository {
    void createIndexIfAbsent();

    void recreateIndex();

    void applyCatalogItem(CatalogItemSearchProjection projection);

    void applyBranchItem(java.util.UUID itemId, BranchSearchProjection projection);

    JsonNode search(ItemSearchCriteria criteria);
}
