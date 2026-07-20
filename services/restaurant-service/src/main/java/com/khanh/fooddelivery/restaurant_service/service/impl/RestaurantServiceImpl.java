package com.khanh.fooddelivery.restaurant_service.service.impl;

import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantStatusHistoryResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantSummaryResponse;
import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantStatusHistory;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantStatus;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.mapper.RestaurantMapper;
import com.khanh.fooddelivery.restaurant_service.mapper.RestaurantStatusHistoryMapper;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantStatusHistoryRepository;
import com.khanh.fooddelivery.restaurant_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantAuthorizationService;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantService;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantServiceImpl implements RestaurantService {
    private static final RestaurantMemberRole[] MANAGE = {
        RestaurantMemberRole.OWNER, RestaurantMemberRole.MANAGER
    };
    private final RestaurantRepository repository;
    private final RestaurantStatusHistoryRepository histories;
    private final RestaurantMapper mapper;
    private final RestaurantStatusHistoryMapper historyMapper;
    private final RestaurantAuthorizationService authorization;
    private final CurrentUserProvider currentUser;

    @Transactional(readOnly = true)
    public List<RestaurantSummaryResponse> mine(Jwt jwt) {
        return mapper.toSummaries(
                repository.findAllByOwnerUserIdOrderByCreatedAtDesc(
                        currentUser.getCurrentUserId(jwt)));
    }

    @Transactional(readOnly = true)
    public RestaurantResponse get(Jwt jwt, UUID id, boolean admin) {
        Restaurant e = required(id);
        if (!admin)
            authorization.requireRestaurantAccess(
                    id, currentUser.getCurrentUserId(jwt), RestaurantMemberRole.values());
        return mapper.toResponse(e);
    }

    public RestaurantResponse update(Jwt jwt, UUID id, RestaurantUpdateRequest r) {
        Restaurant e = managed(jwt, id);
        mapper.update(r, e);
        return mapper.toResponse(e);
    }

    public RestaurantResponse activate(Jwt jwt, UUID id) {
        Restaurant e = managed(jwt, id);
        if (e.getStatus() != RestaurantStatus.PENDING && e.getStatus() != RestaurantStatus.INACTIVE)
            invalid(e);
        return change(e, RestaurantStatus.ACTIVE, null, currentUser.getCurrentUserId(jwt));
    }

    public RestaurantResponse deactivate(Jwt jwt, UUID id) {
        Restaurant e = managed(jwt, id);
        if (e.getStatus() != RestaurantStatus.ACTIVE) invalid(e);
        return change(e, RestaurantStatus.INACTIVE, null, currentUser.getCurrentUserId(jwt));
    }

    public RestaurantResponse close(Jwt jwt, UUID id, String reason) {
        Restaurant e = managed(jwt, id);
        if (!EnumSet.of(
                        RestaurantStatus.PENDING,
                        RestaurantStatus.ACTIVE,
                        RestaurantStatus.INACTIVE)
                .contains(e.getStatus())) invalid(e);
        return change(e, RestaurantStatus.CLOSED, reason, currentUser.getCurrentUserId(jwt));
    }

    public RestaurantResponse suspend(Jwt jwt, UUID id, String reason) {
        Restaurant e = required(id);
        if (e.getStatus() != RestaurantStatus.ACTIVE && e.getStatus() != RestaurantStatus.INACTIVE)
            invalid(e);
        return change(e, RestaurantStatus.SUSPENDED, reason, currentUser.getCurrentUserId(jwt));
    }

    public RestaurantResponse restore(Jwt jwt, UUID id, RestaurantStatus target, String reason) {
        Restaurant e = required(id);
        if (e.getStatus() != RestaurantStatus.SUSPENDED) invalid(e);
        if (target != RestaurantStatus.ACTIVE && target != RestaurantStatus.INACTIVE)
            throw new AppException(
                    ErrorCode.INVALID_RESTAURANT_STATUS_TRANSITION,
                    "Suspended restaurant can only be restored to ACTIVE or INACTIVE");
        return change(e, target, reason, currentUser.getCurrentUserId(jwt));
    }

    @Transactional(readOnly = true)
    public Page<RestaurantStatusHistoryResponse> history(Jwt jwt, UUID id, Pageable p) {
        authorization.requireRestaurantAccess(
                id,
                currentUser.getCurrentUserId(jwt),
                RestaurantMemberRole.OWNER,
                RestaurantMemberRole.MANAGER);
        return histories
                .findAllByRestaurantIdOrderByChangedAtDesc(id, p)
                .map(historyMapper::toResponse);
    }

    private Restaurant managed(Jwt jwt, UUID id) {
        Restaurant e = required(id);
        authorization.requireRestaurantAccess(id, currentUser.getCurrentUserId(jwt), MANAGE);
        return e;
    }

    private Restaurant required(UUID id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESTAURANT_NOT_FOUND));
    }

    private RestaurantResponse change(
            Restaurant e, RestaurantStatus next, String reason, UUID actor) {
        RestaurantStatus old = e.getStatus();
        e.setStatus(next);
        RestaurantStatusHistory h = new RestaurantStatusHistory();
        h.setRestaurant(e);
        h.setOldStatus(old);
        h.setNewStatus(next);
        h.setReason(reason);
        h.setChangedByUserId(actor);
        histories.save(h);
        return mapper.toResponse(e);
    }

    private void invalid(Restaurant e) {
        throw new AppException(
                ErrorCode.INVALID_RESTAURANT_STATUS_TRANSITION,
                "Cannot transition restaurant from " + e.getStatus());
    }
}
