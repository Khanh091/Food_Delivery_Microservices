package com.khanh.fooddelivery.catalog_service.service.impl;

import com.khanh.fooddelivery.catalog_service.dto.request.MenuCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.MenuResponse;
import com.khanh.fooddelivery.catalog_service.entity.Menu;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.mapper.MenuMapper;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogEventData;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogEventType;
import com.khanh.fooddelivery.catalog_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.catalog_service.repository.MenuRepository;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import com.khanh.fooddelivery.catalog_service.service.MenuService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuServiceImpl implements MenuService {
    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;
    private final CatalogAuthorizationService authorizationService;
    private final OutboxEventService outboxEventService;

    @Override
    public MenuResponse create(MenuCreateRequest request) {
        validateDates(request.availableFrom(), request.availableUntil());
        authorizationService.requireBranchCatalogAccess(request.restaurantId(), request.branchId());
        Menu menu = menuMapper.toEntity(request);
        menu.setStatus(CatalogStatus.ACTIVE);
        Menu savedMenu = menuRepository.save(menu);
        enqueue(savedMenu, "CREATED");
        return menuMapper.toResponse(savedMenu);
    }

    @Override
    @Transactional(readOnly = true)
    public MenuResponse get(UUID menuId) {
        Menu menu = required(menuId);
        authorize(menu);
        return menuMapper.toResponse(menu);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuResponse> list(UUID restaurantId, UUID branchId) {
        authorizationService.requireBranchCatalogAccess(restaurantId, branchId);
        return menuMapper.toResponses(
                menuRepository.findAllByRestaurantIdAndBranchIdOrderByCreatedAtAsc(
                        restaurantId, branchId));
    }

    @Override
    public MenuResponse update(UUID menuId, MenuUpdateRequest request) {
        Menu menu = required(menuId);
        authorize(menu);
        validateDates(
                request.availableFrom() == null ? menu.getAvailableFrom() : request.availableFrom(),
                request.availableUntil() == null
                        ? menu.getAvailableUntil()
                        : request.availableUntil());
        menuMapper.update(request, menu);
        Menu savedMenu = menuRepository.save(menu);
        enqueue(savedMenu, "UPDATED");
        return menuMapper.toResponse(savedMenu);
    }

    @Override
    public MenuResponse activate(UUID menuId) {
        return changeStatus(menuId, CatalogStatus.ACTIVE);
    }

    @Override
    public MenuResponse deactivate(UUID menuId) {
        return changeStatus(menuId, CatalogStatus.INACTIVE);
    }

    @Override
    public void delete(UUID menuId) {
        Menu menu = required(menuId);
        authorize(menu);
        menuRepository.delete(menu);
        enqueue(menu, "DELETED");
    }

    private MenuResponse changeStatus(UUID menuId, CatalogStatus status) {
        Menu menu = required(menuId);
        authorize(menu);
        if (menu.getStatus() == status) {
            return menuMapper.toResponse(menu);
        }
        menu.setStatus(status);
        Menu savedMenu = menuRepository.save(menu);
        enqueue(savedMenu, status.name());
        return menuMapper.toResponse(savedMenu);
    }

    private Menu required(UUID menuId) {
        return menuRepository
                .findById(menuId)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_NOT_FOUND));
    }

    private void authorize(Menu menu) {
        authorizationService.requireBranchCatalogAccess(menu.getRestaurantId(), menu.getBranchId());
    }

    private void validateDates(LocalDate availableFrom, LocalDate availableUntil) {
        if (availableFrom != null
                && availableUntil != null
                && availableFrom.isAfter(availableUntil)) {
            throw new AppException(ErrorCode.INVALID_MENU_DATE_RANGE);
        }
    }

    private void enqueue(Menu menu, String action) {
        outboxEventService.enqueue(
                CatalogEventType.MENU_CHANGED,
                "MENU",
                menu.getId(),
                CatalogEventData.menu(menu, action));
    }
}
