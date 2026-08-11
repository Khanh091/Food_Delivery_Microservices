package com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog;

import java.util.UUID;

public record PublicItemImageResponse(
        UUID id, String imageUrl, Integer sortOrder, Boolean isPrimary) {}
