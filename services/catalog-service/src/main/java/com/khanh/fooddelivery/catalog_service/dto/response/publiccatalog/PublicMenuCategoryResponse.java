package com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog;

import java.util.List;
import java.util.UUID;

public record PublicMenuCategoryResponse(
        UUID id,
        String name,
        String description,
        Integer sortOrder,
        List<PublicCatalogItemResponse> items) {}
