package com.khanh.fooddelivery.catalog_service.dto.response;

import java.util.List;

public record OptionTemplatePageResponse(
        List<OptionTemplateResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {}
