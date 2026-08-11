package com.khanh.fooddelivery.catalog_service.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.entity.ItemImage;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.mapper.ItemImageMapper;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.ItemImageRepository;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import com.khanh.fooddelivery.catalog_service.storage.ImageUploadValidator;
import com.khanh.fooddelivery.catalog_service.storage.StorageProperties;
import com.khanh.fooddelivery.catalog_service.storage.StorageProvider;
import com.khanh.fooddelivery.catalog_service.storage.StorageService;
import com.khanh.fooddelivery.catalog_service.storage.StorageUploadResult;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ItemImageServiceImplTests {
    private final UUID itemId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();
    @Mock private CatalogItemRepository itemRepository;
    @Mock private ItemImageRepository imageRepository;
    @Mock private ItemImageMapper imageMapper;
    @Mock private CatalogAuthorizationService authorizationService;
    @Mock private StorageService storageService;
    @Mock private ImageUploadValidator imageUploadValidator;
    private ItemImageServiceImpl service;
    private MockMultipartFile file;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        service =
                new ItemImageServiceImpl(
                        itemRepository,
                        imageRepository,
                        imageMapper,
                        authorizationService,
                        storageService,
                        properties,
                        imageUploadValidator);
        file = new MockMultipartFile("file", "item.png", "image/png", new byte[] {1});
    }

    @Test
    void firstImageIsPrimaryAndPersistsStorageMetadata() {
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item()));
        when(imageRepository.existsByItemId(itemId)).thenReturn(false);
        when(storageService.upload(any(), any(), any())).thenReturn(uploadResult());
        when(imageRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.upload(itemId, file, 0, false);

        ArgumentCaptor<ItemImage> imageCaptor = ArgumentCaptor.forClass(ItemImage.class);
        verify(imageRepository).saveAndFlush(imageCaptor.capture());
        ItemImage saved = imageCaptor.getValue();
        assertEquals(Boolean.TRUE, saved.getIsPrimary());
        assertEquals("image:catalog/items/image-key", saved.getStorageKey());
        assertEquals(StorageProvider.CLOUDINARY, saved.getStorageProvider());
        verify(authorizationService).requireRestaurantCatalogAccess(restaurantId);
    }

    @Test
    void unauthorizedUploadDoesNotCallStorage() {
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item()));
        doThrow(new AppException(ErrorCode.ACCESS_DENIED))
                .when(authorizationService)
                .requireRestaurantCatalogAccess(restaurantId);

        assertThrows(AppException.class, () -> service.upload(itemId, file, 0, false));

        verifyNoInteractions(imageUploadValidator, storageService, imageRepository, imageMapper);
    }

    @Test
    void invalidImageDoesNotCallStorage() {
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item()));
        doThrow(new AppException(ErrorCode.INVALID_IMAGE_FILE))
                .when(imageUploadValidator)
                .validate(file);

        AppException exception =
                assertThrows(AppException.class, () -> service.upload(itemId, file, 0, false));

        assertEquals(ErrorCode.INVALID_IMAGE_FILE, exception.getErrorCode());
        verifyNoInteractions(storageService, imageRepository, imageMapper);
    }

    @Test
    void requestedPrimaryClearsExistingPrimary() {
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item()));
        when(storageService.upload(any(), any(), any())).thenReturn(uploadResult());
        when(imageRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.upload(itemId, file, 1, true);

        verify(imageRepository).clearPrimaryByItemId(itemId);
    }

    @Test
    void setPrimaryClearsOldPrimaryAndSelectsRequestedImage() {
        ItemImage image = image(false);
        when(imageRepository.findByIdAndItemId(image.getId(), itemId))
                .thenReturn(Optional.of(image));
        when(imageRepository.save(image)).thenReturn(image);

        service.setPrimary(itemId, image.getId());

        assertEquals(Boolean.TRUE, image.getIsPrimary());
        verify(imageRepository).clearPrimaryByItemId(itemId);
        verify(authorizationService).requireRestaurantCatalogAccess(restaurantId);
    }

    @Test
    void imageOutsideNestedItemScopeIsNotFound() {
        UUID imageId = UUID.randomUUID();
        when(imageRepository.findByIdAndItemId(imageId, itemId)).thenReturn(Optional.empty());

        AppException exception =
                assertThrows(AppException.class, () -> service.setPrimary(itemId, imageId));

        assertEquals(ErrorCode.ITEM_IMAGE_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(authorizationService, storageService);
    }

    @Test
    void updateSortOrderUsesNestedPersistedImage() {
        ItemImage image = image(false);
        when(imageRepository.findByIdAndItemId(image.getId(), itemId))
                .thenReturn(Optional.of(image));
        when(imageRepository.save(image)).thenReturn(image);

        service.updateSortOrder(itemId, image.getId(), 3);

        assertEquals(3, image.getSortOrder());
        verify(authorizationService).requireRestaurantCatalogAccess(restaurantId);
    }

    @Test
    void deleteNonPrimaryDeletesStorageThenMetadata() {
        ItemImage image = image(false);
        when(imageRepository.findByIdAndItemId(image.getId(), itemId))
                .thenReturn(Optional.of(image));
        when(storageService.getProvider()).thenReturn(StorageProvider.CLOUDINARY);
        doNothing().when(storageService).delete(image.getStorageKey());

        service.delete(itemId, image.getId());

        verify(storageService).delete(image.getStorageKey());
        verify(imageRepository).delete(image);
        verify(imageRepository).flush();
        verify(imageRepository, never()).findFirstByItemIdOrderBySortOrderAscCreatedAtAsc(itemId);
    }

    @Test
    void deletePrimaryPromotesNextImage() {
        ItemImage deleted = image(true);
        ItemImage replacement = image(false);
        when(imageRepository.findByIdAndItemId(deleted.getId(), itemId))
                .thenReturn(Optional.of(deleted));
        when(storageService.getProvider()).thenReturn(StorageProvider.CLOUDINARY);
        when(imageRepository.findFirstByItemIdOrderBySortOrderAscCreatedAtAsc(itemId))
                .thenReturn(Optional.of(replacement));
        when(imageRepository.save(replacement)).thenReturn(replacement);

        service.delete(itemId, deleted.getId());

        assertEquals(Boolean.TRUE, replacement.getIsPrimary());
        verify(imageRepository).save(replacement);
    }

    @Test
    void databaseFailureAfterUploadAttemptsCompensatingDelete() {
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item()));
        when(imageRepository.existsByItemId(itemId)).thenReturn(false);
        StorageUploadResult uploaded = uploadResult();
        when(storageService.upload(any(), any(), any())).thenReturn(uploaded);
        when(imageRepository.saveAndFlush(any()))
                .thenThrow(new RuntimeException("database failure"));

        assertThrows(RuntimeException.class, () -> service.upload(itemId, file, 0, false));

        verify(storageService).delete(uploaded.storageKey());
    }

    @Test
    void storageDeleteFailureDoesNotDeleteMetadata() {
        ItemImage image = image(false);
        when(imageRepository.findByIdAndItemId(image.getId(), itemId))
                .thenReturn(Optional.of(image));
        when(storageService.getProvider()).thenReturn(StorageProvider.CLOUDINARY);
        doThrow(new AppException(ErrorCode.IMAGE_DELETE_FAILED))
                .when(storageService)
                .delete(image.getStorageKey());

        AppException exception =
                assertThrows(AppException.class, () -> service.delete(itemId, image.getId()));

        assertEquals(ErrorCode.IMAGE_DELETE_FAILED, exception.getErrorCode());
        verify(imageRepository, never()).delete(any());
    }

    private CatalogItem item() {
        CatalogItem item = new CatalogItem();
        item.setId(itemId);
        item.setRestaurantId(restaurantId);
        return item;
    }

    private ItemImage image(boolean primary) {
        ItemImage image = new ItemImage();
        image.setId(UUID.randomUUID());
        image.setItem(item());
        image.setImageUrl("https://storage.example/item.png");
        image.setStorageProvider(StorageProvider.CLOUDINARY);
        image.setStorageKey("image:catalog/items/image-key");
        image.setSortOrder(0);
        image.setIsPrimary(primary);
        return image;
    }

    private StorageUploadResult uploadResult() {
        return new StorageUploadResult(
                StorageProvider.CLOUDINARY,
                "image:catalog/items/image-key",
                "http://storage.example/item.png",
                "https://storage.example/item.png");
    }
}
