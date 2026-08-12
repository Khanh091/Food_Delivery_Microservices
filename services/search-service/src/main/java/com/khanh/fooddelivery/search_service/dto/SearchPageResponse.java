package com.khanh.fooddelivery.search_service.dto;

import java.util.List;

public record SearchPageResponse<T>(
        List<T> items, int page, int size, long totalElements, int totalPages) {}
