package com.khanh.fooddelivery.restaurant_service.service.impl;

import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantResponse;
import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.mapper.RestaurantMapper;
import com.khanh.fooddelivery.restaurant_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.restaurant_service.outbox.RestaurantEventData;
import com.khanh.fooddelivery.restaurant_service.outbox.RestaurantEventType;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import com.khanh.fooddelivery.restaurant_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantAuthorizationService;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantMediaService;
import com.khanh.fooddelivery.restaurant_service.storage.FileStorageProperties;
import com.khanh.fooddelivery.restaurant_service.storage.FileStorageService;
import com.khanh.fooddelivery.restaurant_service.storage.StorageProvider;
import com.khanh.fooddelivery.restaurant_service.storage.StorageUploadResult;
import com.khanh.fooddelivery.restaurant_service.storage.exception.FileStorageException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantMediaServiceImpl implements RestaurantMediaService {

    private static final RestaurantMemberRole[] MANAGE = {
        RestaurantMemberRole.OWNER, RestaurantMemberRole.MANAGER
    };
    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;

    private final RestaurantRepository restaurants;
    private final RestaurantMapper restaurantMapper;
    private final RestaurantAuthorizationService authorization;
    private final CurrentUserProvider currentUser;
    private final FileStorageService fileStorageService;
    private final FileStorageProperties storageProperties;
    private final OutboxEventService outbox;

    @Override
    public RestaurantResponse updateLogo(Jwt jwt, UUID restaurantId, MultipartFile file) {
        return updateMedia(jwt, restaurantId, file, MediaKind.LOGO);
    }

    @Override
    public RestaurantResponse updateCover(Jwt jwt, UUID restaurantId, MultipartFile file) {
        return updateMedia(jwt, restaurantId, file, MediaKind.COVER);
    }

    private RestaurantResponse updateMedia(
            Jwt jwt, UUID restaurantId, MultipartFile file, MediaKind kind) {
        Restaurant restaurant = restaurantForUpdate(restaurantId);
        authorization.requireRestaurantAccess(
                restaurantId, currentUser.getCurrentUserId(jwt), MANAGE);
        validateImage(file);

        String folder = buildRestaurantMediaFolder(restaurantId);
        String resourceName = kind.folderName + "-" + restaurantId + "-" + UUID.randomUUID();
        StorageUploadResult upload = fileStorageService.upload(file, folder, resourceName);
        String previousStorageKey =
                kind == MediaKind.LOGO
                        ? restaurant.getLogoStorageKey()
                        : restaurant.getCoverImageStorageKey();

        try {
            if (kind == MediaKind.LOGO) {
                restaurant.setLogoUrl(preferredUrl(upload));
                restaurant.setLogoStorageKey(upload.storageKey());
            } else {
                restaurant.setCoverImageUrl(preferredUrl(upload));
                restaurant.setCoverImageStorageKey(upload.storageKey());
            }
            restaurants.saveAndFlush(restaurant);
        } catch (RuntimeException databaseException) {
            deleteQuietly(upload.storageKey());
            throw databaseException;
        }

        enqueueUpsert(restaurant, kind);
        if (previousStorageKey != null
                && !previousStorageKey.isBlank()
                && !previousStorageKey.equals(upload.storageKey())) {
            deleteQuietly(previousStorageKey);
        }
        return restaurantMapper.toResponse(restaurant);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new FileStorageException(ErrorCode.FILE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null
                || !ALLOWED_IMAGE_TYPES.contains(contentType.trim().toLowerCase(Locale.ROOT))) {
            throw new FileStorageException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
    }

    private Restaurant restaurantForUpdate(UUID restaurantId) {
        return restaurants
                .findByIdForUpdate(restaurantId)
                .orElseThrow(() -> new AppException(ErrorCode.RESTAURANT_NOT_FOUND));
    }

    private String buildRestaurantMediaFolder(UUID restaurantId) {
        String baseFolder =
                fileStorageService.getProvider() == StorageProvider.CLOUDINARY
                        ? storageProperties.getCloudinary().getBaseFolder()
                        : storageProperties.getS3().getBaseFolder();
        String normalizedBaseFolder =
                baseFolder == null
                        ? ""
                        : baseFolder.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
        return normalizedBaseFolder + "/restaurants/" + restaurantId + "/media";
    }

    private String preferredUrl(StorageUploadResult upload) {
        return upload.secureUrl() == null || upload.secureUrl().isBlank()
                ? upload.url()
                : upload.secureUrl();
    }

    private void enqueueUpsert(Restaurant restaurant, MediaKind kind) {
        outbox.enqueue(
                RestaurantEventType.RESTAURANT_UPSERTED,
                "RESTAURANT",
                restaurant.getId(),
                RestaurantEventData.restaurant(restaurant, kind.action));
    }

    private void deleteQuietly(String storageKey) {
        try {
            fileStorageService.delete(storageKey);
        } catch (RuntimeException cleanupException) {
            log.error(
                    "Restaurant media cleanup failed for storage key {}",
                    storageKey,
                    cleanupException);
        }
    }

    private enum MediaKind {
        LOGO("logo", "LOGO_UPDATED"),
        COVER("cover", "COVER_UPDATED");

        private final String folderName;
        private final String action;

        MediaKind(String folderName, String action) {
            this.folderName = folderName;
            this.action = action;
        }
    }
}