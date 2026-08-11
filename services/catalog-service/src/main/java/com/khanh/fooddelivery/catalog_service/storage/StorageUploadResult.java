package com.khanh.fooddelivery.catalog_service.storage;

public record StorageUploadResult(
        StorageProvider provider, String storageKey, String url, String secureUrl) {}
