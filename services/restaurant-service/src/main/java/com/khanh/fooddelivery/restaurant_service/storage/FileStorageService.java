package com.khanh.fooddelivery.restaurant_service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StorageUploadResult upload(MultipartFile file, String folder, String resourceName);

    void delete(String storageKey);

    StorageProvider getProvider();
}
