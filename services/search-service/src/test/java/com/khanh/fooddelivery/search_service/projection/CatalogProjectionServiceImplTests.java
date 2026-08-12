package com.khanh.fooddelivery.search_service.projection;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.search_service.event.DomainEventEnvelope;
import com.khanh.fooddelivery.search_service.repository.SearchProjectionRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CatalogProjectionServiceImplTests {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void catalogItemEventCreatesTopLevelProjection() {
        SearchProjectionRepository repository = Mockito.mock(SearchProjectionRepository.class);
        CatalogProjectionService service = new CatalogProjectionServiceImpl(repository);

        service.apply(catalogItemEvent(1));

        verify(repository).applyCatalogItem(any());
    }

    @Test
    void branchItemEventCreatesOrUpdatesNestedBranchProjection() {
        SearchProjectionRepository repository = Mockito.mock(SearchProjectionRepository.class);
        CatalogProjectionService service = new CatalogProjectionServiceImpl(repository);
        UUID itemId = UUID.randomUUID();

        service.apply(branchItemEvent(itemId, UUID.randomUUID(), 3));
        service.apply(branchItemEvent(itemId, UUID.randomUUID(), 1));

        verify(repository, Mockito.times(2)).applyBranchItem(eq(itemId), any());
    }

    @Test
    void unknownEventCannotMutateProjection() {
        SearchProjectionRepository repository = Mockito.mock(SearchProjectionRepository.class);
        CatalogProjectionService service = new CatalogProjectionServiceImpl(repository);

        assertThatThrownBy(
                        () ->
                                service.apply(
                                        new DomainEventEnvelope(
                                                UUID.randomUUID(),
                                                "UNKNOWN",
                                                2,
                                                "UNKNOWN",
                                                UUID.randomUUID(),
                                                1,
                                                Instant.now(),
                                                objectMapper.createObjectNode())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private DomainEventEnvelope catalogItemEvent(long version) {
        return new DomainEventEnvelope(
                UUID.randomUUID(),
                "CATALOG_ITEM_UPSERTED",
                2,
                "CATALOG_ITEM",
                UUID.randomUUID(),
                version,
                Instant.now(),
                objectMapper.valueToTree(
                        Map.of(
                                "itemId", UUID.randomUUID(),
                                "restaurantId", UUID.randomUUID(),
                                "name", "Pho Bo",
                                "itemType", "FOOD",
                                "basePrice", 50000,
                                "currency", "VND",
                                "isVegetarian", false,
                                "status", "ACTIVE")));
    }

    private DomainEventEnvelope branchItemEvent(UUID itemId, UUID branchItemId, long version) {
        return new DomainEventEnvelope(
                UUID.randomUUID(),
                "BRANCH_ITEM_UPSERTED",
                2,
                "BRANCH_ITEM",
                branchItemId,
                version,
                Instant.now(),
                objectMapper.valueToTree(
                        Map.of(
                                "itemId", itemId,
                                "branchItemId", branchItemId,
                                "branchId", UUID.randomUUID(),
                                "sellingPrice", 50000,
                                "isAvailable", true)));
    }
}
