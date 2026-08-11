package com.khanh.fooddelivery.catalog_service.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.catalog_service.dto.request.MenuCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.MenuResponse;
import com.khanh.fooddelivery.catalog_service.entity.Menu;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.mapper.MenuMapper;
import com.khanh.fooddelivery.catalog_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.catalog_service.repository.MenuRepository;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTests {
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID menuId = UUID.randomUUID();
    @Mock private MenuRepository menuRepository;
    @Mock private MenuMapper menuMapper;
    @Mock private CatalogAuthorizationService authorizationService;
    @Mock private OutboxEventService outboxEventService;
    private MenuServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new MenuServiceImpl(
                        menuRepository, menuMapper, authorizationService, outboxEventService);
    }

    @Test
    void createAuthorizedMenuSetsActiveStatus() {
        Menu menu = menu();
        when(menuMapper.toEntity(any())).thenReturn(menu);
        when(menuRepository.save(menu)).thenReturn(menu);
        when(menuMapper.toResponse(menu)).thenReturn(response(menu));

        service.create(new MenuCreateRequest(restaurantId, branchId, "Lunch", null, null, null));

        verify(authorizationService).requireBranchCatalogAccess(restaurantId, branchId);
        assertEquals(CatalogStatus.ACTIVE, menu.getStatus());
    }

    @Test
    void createUnauthorizedMenuDoesNotSave() {
        doThrow(new AppException(ErrorCode.ACCESS_DENIED))
                .when(authorizationService)
                .requireBranchCatalogAccess(restaurantId, branchId);

        assertThrows(
                AppException.class,
                () ->
                        service.create(
                                new MenuCreateRequest(
                                        restaurantId, branchId, "Lunch", null, null, null)));

        verify(menuRepository, never()).save(any());
    }

    @Test
    void getUsesPersistedRestaurantAndBranchForAuthorization() {
        Menu menu = menu();
        when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));
        when(menuMapper.toResponse(menu)).thenReturn(response(menu));

        service.get(menuId);

        verify(authorizationService).requireBranchCatalogAccess(restaurantId, branchId);
    }

    @Test
    void updateRejectsInvalidDateRange() {
        Menu menu = menu();
        when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));

        AppException error =
                assertThrows(
                        AppException.class,
                        () ->
                                service.update(
                                        menuId,
                                        new MenuUpdateRequest(
                                                null,
                                                null,
                                                LocalDate.of(2026, 2, 2),
                                                LocalDate.of(2026, 2, 1))));

        assertEquals(ErrorCode.INVALID_MENU_DATE_RANGE, error.getErrorCode());
    }

    @Test
    void deactivateAndDeleteAuthorizeBeforeMutation() {
        Menu menu = menu();
        when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));
        when(menuRepository.save(menu)).thenReturn(menu);
        when(menuMapper.toResponse(menu)).thenReturn(response(menu));

        service.deactivate(menuId);
        service.delete(menuId);

        assertEquals(CatalogStatus.INACTIVE, menu.getStatus());
        verify(menuRepository).delete(menu);
    }

    private Menu menu() {
        Menu menu = new Menu();
        menu.setId(menuId);
        menu.setRestaurantId(restaurantId);
        menu.setBranchId(branchId);
        return menu;
    }

    private MenuResponse response(Menu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getRestaurantId(),
                menu.getBranchId(),
                "Lunch",
                null,
                menu.getStatus(),
                null,
                null,
                null,
                null);
    }
}
