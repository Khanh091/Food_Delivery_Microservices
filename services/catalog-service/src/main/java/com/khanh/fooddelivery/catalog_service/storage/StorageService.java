package com.khanh.fooddelivery.catalog_service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    StorageUploadResult upload(MultipartFile file, String folder, String resourceName);

    void delete(String storageKey);

    StorageProvider getProvider();
}
