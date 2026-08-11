package com.khanh.fooddelivery.catalog_service.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.catalog_service.dto.request.OptionValueCreateRequest;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.entity.OptionGroup;
import com.khanh.fooddelivery.catalog_service.entity.OptionValue;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.mapper.OptionValueMapper;
import com.khanh.fooddelivery.catalog_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.catalog_service.repository.OptionGroupRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionValueRepository;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OptionValueServiceImplTests {
    private final UUID itemId = UUID.randomUUID(),
            groupId = UUID.randomUUID(),
            restaurantId = UUID.randomUUID();
    @Mock private OptionGroupRepository groupRepository;
    @Mock private OptionValueRepository valueRepository;
    @Mock private OptionValueMapper valueMapper;
    @Mock private CatalogAuthorizationService authorizationService;
    @Mock private OutboxEventService outboxEventService;
    private OptionValueServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new OptionValueServiceImpl(
                        groupRepository,
                        valueRepository,
                        valueMapper,
                        authorizationService,
                        outboxEventService);
    }

    @Test
    void createAuthorizesPersistedItemAndDefaultsAvailable() {
        OptionGroup g = group();
        OptionValue v = new OptionValue();
        when(groupRepository.findByIdAndItemId(groupId, itemId)).thenReturn(Optional.of(g));
        when(valueMapper.toEntity(any())).thenReturn(v);
        when(valueRepository.save(v)).thenReturn(v);
        service.create(itemId, groupId, new OptionValueCreateRequest("Large", BigDecimal.TEN, 1));
        verify(authorizationService).requireRestaurantCatalogAccess(restaurantId);
        assertEquals(g, v.getOptionGroup());
        assertEquals(Boolean.TRUE, v.getIsAvailable());
    }

    @Test
    void valueOutsideNestedGroupScopeIsNotFound() {
        UUID otherGroupId = UUID.randomUUID();
        when(groupRepository.findByIdAndItemId(otherGroupId, itemId)).thenReturn(Optional.empty());

        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> service.get(itemId, otherGroupId, UUID.randomUUID()));

        assertEquals(ErrorCode.OPTION_GROUP_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(authorizationService, valueRepository);
    }

    @Test
    void unavailableChangesOnlyAvailability() {
        OptionGroup g = group();
        UUID valueId = UUID.randomUUID();
        OptionValue value = new OptionValue();
        value.setId(valueId);
        value.setOptionGroup(g);
        value.setIsAvailable(true);
        when(groupRepository.findByIdAndItemId(groupId, itemId)).thenReturn(Optional.of(g));
        when(valueRepository.findByIdAndOptionGroupId(valueId, groupId))
                .thenReturn(Optional.of(value));
        when(valueRepository.save(value)).thenReturn(value);

        service.markUnavailable(itemId, groupId, valueId);

        assertEquals(Boolean.FALSE, value.getIsAvailable());
        verify(authorizationService).requireRestaurantCatalogAccess(restaurantId);
    }

    @Test
    void updateKeepsPersistedParentGroup() {
        OptionGroup g = group();
        UUID valueId = UUID.randomUUID();
        OptionValue value = new OptionValue();
        value.setId(valueId);
        value.setOptionGroup(g);
        when(groupRepository.findByIdAndItemId(groupId, itemId)).thenReturn(Optional.of(g));
        when(valueRepository.findByIdAndOptionGroupId(valueId, groupId))
                .thenReturn(Optional.of(value));
        when(valueRepository.save(value)).thenReturn(value);

        service.update(
                itemId,
                groupId,
                valueId,
                new com.khanh.fooddelivery.catalog_service.dto.request.OptionValueUpdateRequest(
                        "Updated", BigDecimal.ONE, 2));

        assertEquals(g, value.getOptionGroup());
        verify(valueMapper).update(any(), any());
    }

    private OptionGroup group() {
        CatalogItem i = new CatalogItem();
        i.setId(itemId);
        i.setRestaurantId(restaurantId);
        OptionGroup g = new OptionGroup();
        g.setId(groupId);
        g.setItem(i);
        return g;
    }
}
