package com.khanh.fooddelivery.restaurant_service.service.impl;

import com.khanh.fooddelivery.restaurant_service.dto.request.AcceptingOrdersUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.BranchBusinessHourRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.BranchBusinessHoursUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.BranchSpecialHourCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.BranchSpecialHourUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantBranchCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantBranchUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.BranchBusinessHourResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.BranchSpecialHourResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBranchResponse;
import com.khanh.fooddelivery.restaurant_service.entity.BranchBusinessHour;
import com.khanh.fooddelivery.restaurant_service.entity.BranchSpecialHour;
import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantBranchStatus;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantStatus;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.mapper.BranchBusinessHourMapper;
import com.khanh.fooddelivery.restaurant_service.mapper.BranchSpecialHourMapper;
import com.khanh.fooddelivery.restaurant_service.mapper.RestaurantBranchMapper;
import com.khanh.fooddelivery.restaurant_service.repository.BranchBusinessHourRepository;
import com.khanh.fooddelivery.restaurant_service.repository.BranchSpecialHourRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBranchRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import com.khanh.fooddelivery.restaurant_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantAuthorizationService;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantBranchService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantBranchServiceImpl implements RestaurantBranchService {
    private static final RestaurantMemberRole[] MANAGE = {
        RestaurantMemberRole.OWNER, RestaurantMemberRole.MANAGER
    };
    private final RestaurantBranchRepository branches;
    private final RestaurantRepository restaurants;
    private final BranchBusinessHourRepository businessHours;
    private final BranchSpecialHourRepository specialHours;
    private final RestaurantBranchMapper branchMapper;
    private final BranchBusinessHourMapper hourMapper;
    private final BranchSpecialHourMapper specialMapper;
    private final RestaurantAuthorizationService authorization;
    private final CurrentUserProvider currentUser;

    public RestaurantBranchResponse create(
            Jwt jwt, UUID restaurantId, RestaurantBranchCreateRequest r) {
        Restaurant restaurant = restaurant(restaurantId);
        accessRestaurant(jwt, restaurantId);
        if (branches.existsByRestaurantIdAndBranchCode(restaurantId, r.branchCode()))
            throw new AppException(ErrorCode.BRANCH_CODE_ALREADY_EXISTS);
        RestaurantBranch b = branchMapper.toEntity(r);
        b.setRestaurant(restaurant);
        b.setStatus(RestaurantBranchStatus.PENDING);
        b.setAcceptingOrders(false);
        return branchMapper.toResponse(branches.save(b));
    }

    @Transactional(readOnly = true)
    public List<RestaurantBranchResponse> list(Jwt jwt, UUID restaurantId) {
        accessRestaurant(jwt, restaurantId);
        return branchMapper.toResponses(
                branches.findAllByRestaurantIdOrderByCreatedAtAsc(restaurantId));
    }

    @Transactional(readOnly = true)
    public RestaurantBranchResponse get(Jwt jwt, UUID id) {
        RestaurantBranch b = branch(id);
        accessBranch(jwt, id);
        return branchMapper.toResponse(b);
    }

    public RestaurantBranchResponse update(Jwt jwt, UUID id, RestaurantBranchUpdateRequest r) {
        RestaurantBranch b = branch(id);
        accessBranch(jwt, id);
        branchMapper.update(r, b);
        if (b.getStatus() != RestaurantBranchStatus.ACTIVE) b.setAcceptingOrders(false);
        return branchMapper.toResponse(b);
    }

    public void close(Jwt jwt, UUID id) {
        RestaurantBranch b = branch(id);
        accessBranch(jwt, id);
        b.setStatus(RestaurantBranchStatus.CLOSED);
        b.setAcceptingOrders(false);
    }

    public RestaurantBranchResponse acceptingOrders(
            Jwt jwt, UUID id, AcceptingOrdersUpdateRequest r) {
        RestaurantBranch b = branch(id);
        accessBranch(jwt, id);
        if (Boolean.TRUE.equals(r.acceptingOrders())
                && (b.getStatus() != RestaurantBranchStatus.ACTIVE
                        || b.getRestaurant().getStatus() != RestaurantStatus.ACTIVE))
            throw new AppException(
                    ErrorCode.BRANCH_NOT_ACTIVE, "Restaurant and branch must both be ACTIVE");
        b.setAcceptingOrders(r.acceptingOrders());
        return branchMapper.toResponse(b);
    }

    public List<BranchBusinessHourResponse> setHours(
            Jwt jwt, UUID id, BranchBusinessHoursUpdateRequest r) {
        RestaurantBranch b = branch(id);
        accessBranch(jwt, id);
        Set<Short> days = new HashSet<>();
        for (BranchBusinessHourRequest item : r.hours()) {
            if (!days.add(item.dayOfWeek()))
                throw new AppException(
                        ErrorCode.INVALID_BUSINESS_HOURS,
                        "Duplicate dayOfWeek: " + item.dayOfWeek());
            validHours(item.isClosed(), item.openTime(), item.closeTime());
            BranchBusinessHour h =
                    businessHours
                            .findByBranchIdAndDayOfWeek(id, item.dayOfWeek())
                            .orElseGet(BranchBusinessHour::new);
            h.setBranch(b);
            h.setDayOfWeek(item.dayOfWeek());
            h.setClosed(item.isClosed());
            h.setOpenTime(item.isClosed() ? null : item.openTime());
            h.setCloseTime(item.isClosed() ? null : item.closeTime());
            businessHours.save(h);
        }
        businessHours.flush();
        return hourMapper.toResponses(businessHours.findAllByBranchIdOrderByDayOfWeek(id));
    }

    @Transactional(readOnly = true)
    public List<BranchBusinessHourResponse> hours(Jwt jwt, UUID id) {
        branch(id);
        accessBranch(jwt, id);
        return hourMapper.toResponses(businessHours.findAllByBranchIdOrderByDayOfWeek(id));
    }

    public BranchSpecialHourResponse addSpecial(
            Jwt jwt, UUID id, BranchSpecialHourCreateRequest r) {
        RestaurantBranch b = branch(id);
        accessBranch(jwt, id);
        validHours(r.isClosed(), r.openTime(), r.closeTime());
        if (specialHours.existsByBranchIdAndSpecialDate(id, r.specialDate()))
            throw new AppException(
                    ErrorCode.COMMON_CONFLICT, "Special hours already exist for date");
        BranchSpecialHour h = specialMapper.toEntity(r);
        h.setBranch(b);
        h.setClosed(r.isClosed());
        normalize(h);
        return specialMapper.toResponse(specialHours.save(h));
    }

    @Transactional(readOnly = true)
    public List<BranchSpecialHourResponse> specials(Jwt jwt, UUID id) {
        branch(id);
        accessBranch(jwt, id);
        return specialMapper.toResponses(specialHours.findAllByBranchIdOrderBySpecialDateAsc(id));
    }

    public BranchSpecialHourResponse updateSpecial(
            Jwt jwt, UUID id, UUID sid, BranchSpecialHourUpdateRequest r) {
        branch(id);
        accessBranch(jwt, id);
        BranchSpecialHour h =
                specialHours
                        .findByIdAndBranchId(sid, id)
                        .orElseThrow(
                                () ->
                                        new AppException(
                                                ErrorCode.BRANCH_NOT_FOUND,
                                                "Special hours not found"));
        specialMapper.update(r, h);
        if (r.isClosed() != null) h.setClosed(r.isClosed());
        validHours(h.isClosed(), h.getOpenTime(), h.getCloseTime());
        normalize(h);
        return specialMapper.toResponse(h);
    }

    public void deleteSpecial(Jwt jwt, UUID id, UUID sid) {
        branch(id);
        accessBranch(jwt, id);
        specialHours.delete(
                specialHours
                        .findByIdAndBranchId(sid, id)
                        .orElseThrow(
                                () ->
                                        new AppException(
                                                ErrorCode.BRANCH_NOT_FOUND,
                                                "Special hours not found")));
    }

    private void validHours(Boolean closed, java.time.LocalTime open, java.time.LocalTime close) {
        if (closed == null
                || (closed
                        ? (open != null || close != null)
                        : (open == null || close == null || !close.isAfter(open))))
            throw new AppException(ErrorCode.INVALID_BUSINESS_HOURS);
    }

    private void normalize(BranchSpecialHour h) {
        if (h.isClosed()) {
            h.setOpenTime(null);
            h.setCloseTime(null);
        }
    }

    private Restaurant restaurant(UUID id) {
        return restaurants
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESTAURANT_NOT_FOUND));
    }

    private RestaurantBranch branch(UUID id) {
        return branches.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
    }

    private void accessRestaurant(Jwt jwt, UUID id) {
        authorization.requireRestaurantAccess(id, currentUser.getCurrentUserId(jwt), MANAGE);
    }

    private void accessBranch(Jwt jwt, UUID id) {
        authorization.requireBranchAccess(id, currentUser.getCurrentUserId(jwt), MANAGE);
    }
}
