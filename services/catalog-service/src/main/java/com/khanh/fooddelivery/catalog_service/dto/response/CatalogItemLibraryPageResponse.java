package com.khanh.fooddelivery.catalog_service.dto.response;

import java.util.List;

public record CatalogItemLibraryPageResponse(
        List<CatalogItemLibraryItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {}
