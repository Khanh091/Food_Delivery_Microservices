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
import java.util.Optional;
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
            return unavailable("RESTAURANT_NOT_ACTIVE", b.isAcceptingOrders());
        if (b.getStatus() != RestaurantBranchStatus.ACTIVE)
            return unavailable("BRANCH_NOT_ACTIVE", b.isAcceptingOrders());
        LocalDate date = now.toLocalDate();
        LocalTime time = now.toLocalTime();
        Optional<Schedule> current = schedule(id, date);
        if (current.isPresent() && current.get().special()) {
            return evaluateCurrentDay(current.get(), time, b.isAcceptingOrders());
        }
        if (current.isPresent() && isWithinCurrentDay(current.get(), time)) {
            return available(current.get(), true, b.isAcceptingOrders());
        }
        Optional<Schedule> previous = schedule(id, date.minusDays(1));
        if (previous.isPresent() && isWithinPreviousDayCarryOver(previous.get(), time)) {
            return available(previous.get(), true, b.isAcceptingOrders());
        }
        return current
                .map(schedule -> available(schedule, false, b.isAcceptingOrders()))
                .orElse(closedToday("NO_BUSINESS_HOURS", b.isAcceptingOrders()));
    }

    private BranchOperatingStatusResponse evaluateCurrentDay(
            Schedule schedule, LocalTime now, boolean acceptingOrders) {
        return available(schedule, isWithinCurrentDay(schedule, now), acceptingOrders);
    }

    private BranchOperatingStatusResponse available(
            Schedule schedule, boolean withinBusinessHours, boolean acceptingOrders) {
        boolean operating = withinBusinessHours && acceptingOrders;
        return new BranchOperatingStatusResponse(
                operating,
                withinBusinessHours,
                acceptingOrders,
                schedule.closed(),
                schedule.open(),
                schedule.close(),
                operating
                        ? "OPEN"
                        : withinBusinessHours && !acceptingOrders
                                ? "NOT_ACCEPTING_ORDERS"
                                : schedule.reason());
    }

    private Optional<Schedule> schedule(UUID branchId, LocalDate date) {
        return specials.findByBranchIdAndSpecialDate(branchId, date)
                .map(hour -> new Schedule(hour.isClosed(), hour.getOpenTime(), hour.getCloseTime(), "SPECIAL_HOURS_CLOSED", true))
                .or(() -> hours.findByBranchIdAndDayOfWeek(branchId, (short) date.getDayOfWeek().getValue())
                        .map(hour -> new Schedule(hour.isClosed(), hour.getOpenTime(), hour.getCloseTime(), "BUSINESS_HOURS_CLOSED", false)));
    }

    private boolean isWithinCurrentDay(Schedule schedule, LocalTime time) {
        if (schedule.closed() || schedule.open() == null || schedule.close() == null) return false;
        return schedule.close().isAfter(schedule.open())
                ? !time.isBefore(schedule.open()) && time.isBefore(schedule.close())
                : !time.isBefore(schedule.open());
    }

    private boolean isWithinPreviousDayCarryOver(Schedule schedule, LocalTime time) {
        return !schedule.closed()
                && schedule.open() != null
                && schedule.close() != null
                && schedule.close().isBefore(schedule.open())
                && time.isBefore(schedule.close());
    }

    private BranchOperatingStatusResponse unavailable(String reason, boolean acceptingOrders) {
        return new BranchOperatingStatusResponse(false, false, acceptingOrders, false, null, null, reason);
    }

    private BranchOperatingStatusResponse closedToday(String reason, boolean acceptingOrders) {
        return new BranchOperatingStatusResponse(false, false, acceptingOrders, true, null, null, reason);
    }

    private record Schedule(boolean closed, LocalTime open, LocalTime close, String reason, boolean special) {}
}
