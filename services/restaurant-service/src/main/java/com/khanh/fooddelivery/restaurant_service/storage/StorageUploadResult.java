package com.khanh.fooddelivery.restaurant_service.storage;

public record StorageUploadResult(
        StorageProvider provider,
        String storageKey,
        String url,
        String secureUrl,
        String originalFileName,
        String contentType,
        long fileSize) {}
