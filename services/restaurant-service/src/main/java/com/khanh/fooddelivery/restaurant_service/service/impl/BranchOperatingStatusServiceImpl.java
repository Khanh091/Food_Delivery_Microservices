package com.khanh.fooddelivery.restaurant_service.service.impl;

import com.khanh.fooddelivery.restaurant_service.dto.response.BranchOperatingStatusResponse;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantBranchStatus;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantStatus;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.repository.BranchBusinessHourRepository;
import com.khanh.fooddelivery.restaurant_service.repository.BranchSpecialHourRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBranchRepository;
import com.khanh.fooddelivery.restaurant_service.service.BranchOperatingStatusService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchOperatingStatusServiceImpl implements BranchOperatingStatusService {
    private final RestaurantBranchRepository branches;
    private final BranchSpecialHourRepository specials;
    private final BranchBusinessHourRepository hours;

    public BranchOperatingStatusResponse getOperatingStatus(UUID id, ZonedDateTime now) {
        RestaurantBranch b =
                branches.findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
        if (b.getRestaurant().getStatus() != RestaurantStatus.ACTIVE)
            return closed("RESTAURANT_NOT_ACTIVE");
        if (b.getStatus() != RestaurantBranchStatus.ACTIVE) return closed("BRANCH_NOT_ACTIVE");
        if (!b.isAcceptingOrders()) return closed("NOT_ACCEPTING_ORDERS");
        LocalDate date = now.toLocalDate();
        LocalTime time = now.toLocalTime();
        return specials.findByBranchIdAndSpecialDate(id, date)
                .map(
                        h ->
                                evaluate(
                                        h.isClosed(),
                                        h.getOpenTime(),
                                        h.getCloseTime(),
                                        time,
                                        "SPECIAL_HOURS_CLOSED"))
                .orElseGet(
                        () ->
                                hours.findByBranchIdAndDayOfWeek(
                                                id, (short) now.getDayOfWeek().getValue())
                                        .map(
                                                h ->
                                                        evaluate(
                                                                h.isClosed(),
                                                                h.getOpenTime(),
                                                                h.getCloseTime(),
                                                                time,
                                                                "BUSINESS_HOURS_CLOSED"))
                                        .orElse(closed("NO_BUSINESS_HOURS")));
    }

    private BranchOperatingStatusResponse evaluate(
            boolean closed, LocalTime open, LocalTime close, LocalTime now, String reason) {
        return !closed && !now.isBefore(open) && now.isBefore(close)
                ? new BranchOperatingStatusResponse(true, "OPEN")
                : closed(reason);
    }

    private BranchOperatingStatusResponse closed(String r) {
        return new BranchOperatingStatusResponse(false, r);
    }
}
