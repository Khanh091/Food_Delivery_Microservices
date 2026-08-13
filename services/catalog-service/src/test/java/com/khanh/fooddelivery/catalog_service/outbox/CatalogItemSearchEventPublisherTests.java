package com.khanh.fooddelivery.catalog_service.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.entity.ItemImage;
import com.khanh.fooddelivery.catalog_service.enums.CatalogItemType;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import com.khanh.fooddelivery.catalog_service.repository.ItemImageRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CatalogItemSearchEventPublisherTests {
    @Test
    void includesCurrentPrimaryImageInExistingCatalogItemUpsertEvent() {
        ItemImageRepository images = Mockito.mock(ItemImageRepository.class);
        OutboxEventService outbox = Mockito.mock(OutboxEventService.class);
        CatalogItemSearchEventPublisher publisher = new CatalogItemSearchEventPublisher(images, outbox);
        CatalogItem item = item();
        ItemImage primary = new ItemImage();
        primary.setImageUrl("https://images.example/pho-bo.png");
        when(images.findFirstByItemIdAndIsPrimaryTrue(item.getId())).thenReturn(Optional.of(primary));

        publisher.enqueue(CatalogEventType.CATALOG_ITEM_UPSERTED, item, "IMAGE_UPDATED");

        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(outbox)
                .enqueue(
                        eq(CatalogEventType.CATALOG_ITEM_UPSERTED),
                        eq("CATALOG_ITEM"),
                        eq(item.getId()),
                        payload.capture());
        assertThat(payload.getValue()).containsEntry("primaryImageUrl", primary.getImageUrl());
    }

    private CatalogItem item() {
        CatalogItem item = new CatalogItem();
        item.setId(UUID.randomUUID());
        item.setRestaurantId(UUID.randomUUID());
        item.setName("Phở Bò");
        item.setItemType(CatalogItemType.FOOD);
        item.setBasePrice(BigDecimal.valueOf(50000));
        item.setCurrency("VND");
        item.setIsVegetarian(false);
        item.setStatus(CatalogStatus.ACTIVE);
        return item;
    }
}
