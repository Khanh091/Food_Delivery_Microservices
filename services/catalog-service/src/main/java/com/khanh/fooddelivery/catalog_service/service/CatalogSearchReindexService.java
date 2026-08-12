package com.khanh.fooddelivery.catalog_service.service;

import com.khanh.fooddelivery.catalog_service.dto.response.CatalogSearchReindexResponse;

public interface CatalogSearchReindexService {
    CatalogSearchReindexResponse enqueueCurrentCatalogSnapshot();
}
