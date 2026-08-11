package com.khanh.fooddelivery.catalog_service.service;

import com.khanh.fooddelivery.catalog_service.dto.response.ItemImageResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ItemImageService {
    ItemImageResponse upload(
            UUID itemId, MultipartFile file, Integer sortOrder, Boolean requestedPrimary);

    List<ItemImageResponse> list(UUID itemId);

    ItemImageResponse setPrimary(UUID itemId, UUID imageId);

    ItemImageResponse updateSortOrder(UUID itemId, UUID imageId, Integer sortOrder);

    void delete(UUID itemId, UUID imageId);
}
