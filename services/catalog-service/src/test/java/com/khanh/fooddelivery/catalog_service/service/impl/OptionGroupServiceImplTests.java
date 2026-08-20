package com.khanh.fooddelivery.catalog_service.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.catalog_service.dto.request.OptionGroupCreateRequest;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.entity.OptionGroup;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import com.khanh.fooddelivery.catalog_service.enums.OptionSelectionType;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.mapper.OptionGroupMapper;
import com.khanh.fooddelivery.catalog_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionGroupRepository;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OptionGroupServiceImplTests {
    private final UUID itemId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();
    @Mock private CatalogItemRepository itemRepository;
    @Mock private OptionGroupRepository groupRepository;
    @Mock private OptionGroupMapper groupMapper;
    @Mock private CatalogAuthorizationService authorizationService;
    @Mock private OutboxEventService outboxEventService;
    private OptionGroupServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new OptionGroupServiceImpl(
                        itemRepository,
                        groupRepository,
                        groupMapper,
                        authorizationService,
                        outboxEventService);
    }

    @Test
    void singleRequiredIsAccepted() {
        OptionGroup g = group();
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item()));
        when(groupMapper.toEntity(any())).thenReturn(g);
        when(groupRepository.save(g)).thenReturn(g);
        service.create(
                itemId,
                new OptionGroupCreateRequest("Size", OptionSelectionType.SINGLE, 1, 1, true, 0));
        verify(authorizationService).requireRestaurantCatalogAccess(restaurantId);
    }

    @Test
    void singleOptionalIsAccepted() {
        OptionGroup g = group();
        g.setRequired(false);
        g.setMinimumSelections(0);
        g.setMaximumSelections(1);
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item()));
        when(groupMapper.toEntity(any())).thenReturn(g);
        when(groupRepository.save(g)).thenReturn(g);
        service.create(
                itemId,
                new OptionGroupCreateRequest("Size", OptionSelectionType.SINGLE, 0, 1, false, 0));
    }

    @Test
    void invalidSingleMaximumIsRejected() {
        OptionGroup g = group();
        g.setMaximumSelections(3);
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item()));
        when(groupMapper.toEntity(any())).thenReturn(g);
        AppException e =
                assertThrows(
                        AppException.class,
                        () ->
                                service.create(
                                        itemId,
                                        new OptionGroupCreateRequest(
                                                "Size",
                                                OptionSelectionType.SINGLE,
                                                1,
                                                3,
                                                true,
                                                0)));
        assertEquals(ErrorCode.INVALID_OPTION_SELECTION, e.getErrorCode());
    }

    @Test
    void multipleWithMinimumIsRequired() {
        OptionGroup g = group();
        g.setSelectionType(OptionSelectionType.MULTIPLE);
        g.setRequired(false);
        g.setMinimumSelections(2);
        g.setMaximumSelections(3);
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item()));
        when(groupMapper.toEntity(any())).thenReturn(g);
        when(groupRepository.save(g)).thenReturn(g);
        service.create(
                itemId,
                new OptionGroupCreateRequest(
                        "Topping", OptionSelectionType.MULTIPLE, 2, 3, false, 0));
        assertEquals(2, g.getMinimumSelections());
        assertEquals(true, g.getRequired());
    }

    @Test
    void listReturnsOnlyActiveOptionGroups() {
        CatalogItem item = item();
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(groupRepository.findAllByItemIdAndStatusOrderBySortOrderAsc(itemId, CatalogStatus.ACTIVE))
                .thenReturn(List.of());
        when(groupMapper.toResponses(List.of())).thenReturn(List.of());

        service.list(itemId);

        verify(groupRepository).findAllByItemIdAndStatusOrderBySortOrderAsc(itemId, CatalogStatus.ACTIVE);
    }

    @Test
    void unauthorizedCreateDoesNotPersistOptionGroup() {
        CatalogItem item = item();
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        org.mockito.Mockito.doThrow(new AppException(ErrorCode.ACCESS_DENIED))
                .when(authorizationService)
                .requireRestaurantCatalogAccess(restaurantId);

        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                service.create(
                                        itemId,
                                        new OptionGroupCreateRequest(
                                                "Size",
                                                OptionSelectionType.SINGLE,
                                                0,
                                                1,
                                                false,
                                                0)));

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verifyNoInteractions(groupMapper, groupRepository);
    }

    @Test
    void groupOutsideNestedItemScopeIsNotFound() {
        UUID otherItemId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        when(groupRepository.findByIdAndItemId(groupId, otherItemId)).thenReturn(Optional.empty());

        AppException exception =
                assertThrows(AppException.class, () -> service.get(otherItemId, groupId));

        assertEquals(ErrorCode.OPTION_GROUP_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(authorizationService);
    }

    @Test
    void deactivateChangesOnlyGroupStatus() {
        OptionGroup group = group();
        group.setId(UUID.randomUUID());
        when(groupRepository.findByIdAndItemId(group.getId(), itemId))
                .thenReturn(Optional.of(group));
        when(groupRepository.save(group)).thenReturn(group);

        service.deactivate(itemId, group.getId());

        assertEquals(CatalogStatus.INACTIVE, group.getStatus());
        verify(authorizationService).requireRestaurantCatalogAccess(restaurantId);
    }

    private CatalogItem item() {
        CatalogItem i = new CatalogItem();
        i.setId(itemId);
        i.setRestaurantId(restaurantId);
        return i;
    }

    private OptionGroup group() {
        OptionGroup g = new OptionGroup();
        g.setId(UUID.randomUUID());
        g.setItem(item());
        g.setStatus(CatalogStatus.ACTIVE);
        g.setSelectionType(OptionSelectionType.SINGLE);
        g.setRequired(true);
        g.setMinimumSelections(1);
        g.setMaximumSelections(1);
        return g;
    }
}
