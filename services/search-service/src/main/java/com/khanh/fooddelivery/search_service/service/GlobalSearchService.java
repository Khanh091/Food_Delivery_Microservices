package com.khanh.fooddelivery.search_service.service;

import com.khanh.fooddelivery.search_service.dto.GlobalSearchResult;
import com.khanh.fooddelivery.search_service.dto.SearchPageResponse;

public interface GlobalSearchService {
    SearchPageResponse<GlobalSearchResult> search(String query, int page, int size);
}
