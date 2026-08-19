package com.khanh.fooddelivery.restaurant_service.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.mapper.RestaurantMapper;
import com.khanh.fooddelivery.restaurant_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import com.khanh.fooddelivery.restaurant_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantAuthorizationService;
import com.khanh.fooddelivery.restaurant_service.storage.FileStorageProperties;
import com.khanh.fooddelivery.restaurant_service.storage.FileStorageService;
import com.khanh.fooddelivery.restaurant_service.storage.StorageProvider;
import com.khanh.fooddelivery.restaurant_service.storage.StorageUploadResult;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class RestaurantMediaServiceImplTests {
    private static final UUID RESTAURANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private RestaurantRepository restaurants;
    @Mock private RestaurantMapper restaurantMapper;
    @Mock private RestaurantAuthorizationService authorization;
    @Mock private CurrentUserProvider currentUser;
    @Mock private FileStorageService fileStorageService;
    @Mock private FileStorageProperties storageProperties;
    @Mock private OutboxEventService outbox;
    @Mock private Jwt jwt;
    @Mock private MultipartFile file;
    @Mock private StorageUploadResult upload;

    private RestaurantMediaServiceImpl service;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        service = new RestaurantMediaServiceImpl(
                restaurants,
                restaurantMapper,
                authorization,
                currentUser,
                fileStorageService,
                storageProperties,
                outbox);
        restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setOwnerUserId(USER_ID);
    }

    @Test
    void authorizedOwnerUploadsLogoAndReplacesPreviousStorageKey() {
        authorizedRestaurant();
        validImage();
        when(fileStorageService.getProvider()).thenReturn(StorageProvider.CLOUDINARY);
        FileStorageProperties.Cloudinary cloudinary = new FileStorageProperties.Cloudinary();
        cloudinary.setBaseFolder("food-delivery/restaurant-documents");
        when(storageProperties.getCloudinary()).thenReturn(cloudinary);
        when(fileStorageService.upload(any(), anyString(), anyString())).thenReturn(upload);
        when(upload.secureUrl()).thenReturn("https://secure.example/logo.png");
        when(upload.storageKey()).thenReturn("image:new-logo");
        restaurant.setLogoStorageKey("image:old-logo");
        when(restaurants.saveAndFlush(restaurant)).thenReturn(restaurant);

        service.updateLogo(jwt, RESTAURANT_ID, file);

        verify(restaurants).saveAndFlush(restaurant);
        verify(fileStorageService).delete("image:old-logo");
    }

    @Test
    void authorizationDenialSkipsStorageAndDatabaseWrites() {
        authorizedRestaurant();
        doThrow(new AppException(ErrorCode.RESTAURANT_ACCESS_DENIED))
                .when(authorization)
                .requireRestaurantAccess(
                        eq(RESTAURANT_ID),
                        eq(USER_ID),
                        any(RestaurantMemberRole[].class));

        assertThatThrownBy(() -> service.updateLogo(jwt, RESTAURANT_ID, file))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESTAURANT_ACCESS_DENIED);

        verify(fileStorageService, never()).upload(any(), anyString(), anyString());
        verify(restaurants, never()).saveAndFlush(any());
    }

    @Test
    void rejectsNonImageContentTypeBeforeStorageUpload() {
        authorizedRestaurant();
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("application/pdf");

        assertThatThrownBy(() -> service.updateCover(jwt, RESTAURANT_ID, file))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED);

        verify(fileStorageService, never()).upload(any(), anyString(), anyString());
    }

    @Test
    void databaseFailureCompensatesTheNewlyUploadedObject() {
        authorizedRestaurant();
        validImage();
        when(fileStorageService.getProvider()).thenReturn(StorageProvider.S3);
        FileStorageProperties.S3 s3 = new FileStorageProperties.S3();
        s3.setBaseFolder("restaurant-documents");
        when(storageProperties.getS3()).thenReturn(s3);
        when(fileStorageService.upload(any(), anyString(), anyString())).thenReturn(upload);
        when(upload.secureUrl()).thenReturn("https://secure.example/logo.png");
        when(upload.storageKey()).thenReturn("new-key");
        when(restaurants.saveAndFlush(restaurant)).thenThrow(new RuntimeException("db failed"));

        assertThatThrownBy(() -> service.updateLogo(jwt, RESTAURANT_ID, file))
                .isInstanceOf(RuntimeException.class);

        verify(fileStorageService).delete("new-key");
    }

    private void authorizedRestaurant() {
        when(currentUser.getCurrentUserId(jwt)).thenReturn(USER_ID);
        when(restaurants.findByIdForUpdate(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
    }

    private void validImage() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/jpeg");
    }
}