package com.khanh.fooddelivery.user_service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface AvatarStorageService {
    AvatarUploadResult upload(MultipartFile file, String ownerId);
    void delete(String storageKey);
}
