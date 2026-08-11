package com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog;

import java.util.List;
import java.util.UUID;

public record PublicMenuResponse(
        UUID id, String name, String description, List<PublicMenuCategoryResponse> categories) {}
