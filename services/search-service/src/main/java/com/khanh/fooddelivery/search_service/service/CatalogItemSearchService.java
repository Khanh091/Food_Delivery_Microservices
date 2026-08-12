package com.khanh.fooddelivery.search_service.service;

import com.khanh.fooddelivery.search_service.dto.ItemSearchCriteria;
import com.khanh.fooddelivery.search_service.dto.SearchPageResponse;
import com.khanh.fooddelivery.search_service.dto.CatalogItemSearchResponse;

public interface CatalogItemSearchService {
    SearchPageResponse<CatalogItemSearchResponse> search(ItemSearchCriteria criteria);
}
