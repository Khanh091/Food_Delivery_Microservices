package com.khanh.fooddelivery.catalog_service.service.impl;

import com.khanh.fooddelivery.catalog_service.dto.response.ItemImageResponse;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.entity.ItemImage;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.mapper.ItemImageMapper;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogEventType;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogItemSearchEventPublisher;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.ItemImageRepository;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import com.khanh.fooddelivery.catalog_service.service.ItemImageService;
import com.khanh.fooddelivery.catalog_service.storage.ImageUploadValidator;
import com.khanh.fooddelivery.catalog_service.storage.StorageProperties;
import com.khanh.fooddelivery.catalog_service.storage.StorageService;
import com.khanh.fooddelivery.catalog_service.storage.StorageUploadResult;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemImageServiceImpl implements ItemImageService {
    private final CatalogItemRepository itemRepository;
    private final ItemImageRepository imageRepository;
    private final ItemImageMapper imageMapper;
    private final CatalogAuthorizationService authorizationService;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final ImageUploadValidator imageUploadValidator;
    private final CatalogItemSearchEventPublisher catalogItemSearchEventPublisher;

    @Override
    @Transactional
    public ItemImageResponse upload(
            UUID itemId, MultipartFile file, Integer sortOrder, Boolean requestedPrimary) {
        CatalogItem item = requiredItemForUpdate(itemId);
        authorize(item);
        imageUploadValidator.validate(file);

        UUID imageId = UUID.randomUUID();
        StorageUploadResult uploaded =
                storageService.upload(file, buildFolder(itemId), imageId + "-" + UUID.randomUUID());
        boolean primary =
                Boolean.TRUE.equals(requestedPrimary) || !imageRepository.existsByItemId(itemId);
        ItemImage image = new ItemImage();
        image.setId(imageId);
        image.setItem(item);
        image.setImageUrl(preferredUrl(uploaded));
        image.setStorageProvider(uploaded.provider());
        image.setStorageKey(uploaded.storageKey());
        image.setSortOrder(sortOrder == null ? 0 : sortOrder);
        image.setIsPrimary(primary);

        try {
            if (primary) {
                imageRepository.clearPrimaryByItemId(itemId);
            }
            ItemImage saved = imageRepository.saveAndFlush(image);
            if (primary) {
                publishPrimaryImageChanged(item, "IMAGE_UPDATED");
            }
            return imageMapper.toResponse(saved);
        } catch (RuntimeException exception) {
            compensateUpload(imageId, uploaded);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemImageResponse> list(UUID itemId) {
        CatalogItem item = requiredItem(itemId);
        authorize(item);
        return imageMapper.toResponses(
                imageRepository.findAllByItemIdOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(
                        itemId));
    }

    @Override
    @Transactional
    public ItemImageResponse setPrimary(UUID itemId, UUID imageId) {
        requiredImage(itemId, imageId);
        CatalogItem item = requiredItemForUpdate(itemId);
        ItemImage image = requiredImage(itemId, imageId);
        authorize(item);
        if (!Boolean.TRUE.equals(image.getIsPrimary())) {
            imageRepository.clearPrimaryByItemId(itemId);
            image.setIsPrimary(true);
            imageRepository.save(image);
            publishPrimaryImageChanged(item, "IMAGE_UPDATED");
        }
        return imageMapper.toResponse(image);
    }

    @Override
    @Transactional
    public ItemImageResponse updateSortOrder(UUID itemId, UUID imageId, Integer sortOrder) {
        ItemImage image = requiredImage(itemId, imageId);
        authorize(image.getItem());
        image.setSortOrder(sortOrder);
        return imageMapper.toResponse(imageRepository.save(image));
    }

    @Override
    @Transactional
    public void delete(UUID itemId, UUID imageId) {
        requiredImage(itemId, imageId);
        CatalogItem item = requiredItemForUpdate(itemId);
        ItemImage image = requiredImage(itemId, imageId);
        authorize(item);
        requireActiveProvider(image);
        storageService.delete(image.getStorageKey());

        try {
            imageRepository.delete(image);
            imageRepository.flush();
            if (Boolean.TRUE.equals(image.getIsPrimary())) {
                imageRepository
                        .findFirstByItemIdOrderBySortOrderAscCreatedAtAsc(itemId)
                        .ifPresent(
                                replacement -> {
                                    replacement.setIsPrimary(true);
                                    imageRepository.save(replacement);
                                });
                publishPrimaryImageChanged(item, "IMAGE_UPDATED");
            }
        } catch (RuntimeException exception) {
            log.error(
                    "Database cleanup failed after storage delete for item image {}",
                    imageId,
                    exception);
            throw exception;
        }
    }

    private CatalogItem requiredItem(UUID itemId) {
        return itemRepository
                .findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
    }

    private CatalogItem requiredItemForUpdate(UUID itemId) {
        return itemRepository
                .findByIdForUpdate(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
    }

    private void publishPrimaryImageChanged(CatalogItem item, String action) {
        catalogItemSearchEventPublisher.enqueue(CatalogEventType.CATALOG_ITEM_UPSERTED, item, action);
    }

    private ItemImage requiredImage(UUID itemId, UUID imageId) {
        return imageRepository
                .findByIdAndItemId(imageId, itemId)
                .orElseThrow(() -> new AppException(ErrorCode.ITEM_IMAGE_NOT_FOUND));
    }

    private void authorize(CatalogItem item) {
        authorizationService.requireRestaurantCatalogAccess(item.getRestaurantId());
    }

    private String buildFolder(UUID itemId) {
        String folder = storageProperties.getCloudinary().getBaseFolder();
        String baseFolder =
                folder == null
                        ? ""
                        : folder.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
        return baseFolder + "/items/" + itemId;
    }

    private String preferredUrl(StorageUploadResult uploaded) {
        return uploaded.secureUrl() == null || uploaded.secureUrl().isBlank()
                ? uploaded.url()
                : uploaded.secureUrl();
    }

    private void compensateUpload(UUID imageId, StorageUploadResult uploaded) {
        try {
            storageService.delete(uploaded.storageKey());
        } catch (RuntimeException cleanupException) {
            log.error("Storage cleanup failed for item image {}", imageId, cleanupException);
        }
    }

    private void requireActiveProvider(ItemImage image) {
        if (image.getStorageProvider() != storageService.getProvider()) {
            throw new AppException(ErrorCode.IMAGE_DELETE_FAILED);
        }
    }
}
