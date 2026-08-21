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
import java.time.OffsetDateTime;
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
    private static final int MAX_LOOKAHEAD_DAYS = 7;

    private final RestaurantBranchRepository branches;
    private final BranchSpecialHourRepository specials;
    private final BranchBusinessHourRepository hours;

    @Override
    public BranchOperatingStatusResponse getOperatingStatus(UUID id, ZonedDateTime now) {
        RestaurantBranch branch =
                branches.findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
        if (branch.getRestaurant().getStatus() != RestaurantStatus.ACTIVE) {
            return unavailable("RESTAURANT_NOT_ACTIVE", branch.isAcceptingOrders());
        }
        if (branch.getStatus() != RestaurantBranchStatus.ACTIVE) {
            return unavailable("BRANCH_NOT_ACTIVE", branch.isAcceptingOrders());
        }

        ZonedDateTime businessNow = now.withZoneSameInstant(BUSINESS_ZONE);
        LocalDate date = businessNow.toLocalDate();
        LocalTime time = businessNow.toLocalTime();
        Optional<Schedule> today = schedule(id, date);

        if (today.filter(schedule -> isWithinCurrentDay(schedule, time)).isPresent()) {
            return open(today.orElseThrow(), date, branch.isAcceptingOrders());
        }

        if (today.isEmpty() || !today.orElseThrow().special()) {
            Optional<Schedule> previous = schedule(id, date.minusDays(1));
            if (previous.filter(schedule -> isWithinPreviousDayCarryOver(schedule, time)).isPresent()) {
                return open(previous.orElseThrow(), date.minusDays(1), branch.isAcceptingOrders());
            }
        }

        Optional<Opening> nextOpening = findNextOpening(id, date, time);
        if (today.isPresent()) {
            return closed(today.orElseThrow(), nextOpening, branch.isAcceptingOrders());
        }
        return closedToday("NO_BUSINESS_HOURS", nextOpening, branch.isAcceptingOrders());
    }

    private BranchOperatingStatusResponse open(
            Schedule schedule, LocalDate scheduleDate, boolean acceptingOrders) {
        boolean operating = acceptingOrders;
        return new BranchOperatingStatusResponse(
                operating,
                true,
                acceptingOrders,
                false,
                schedule.open(),
                schedule.close(),
                null,
                closeAt(schedule, scheduleDate).orElse(null),
                operating ? "OPEN" : "NOT_ACCEPTING_ORDERS");
    }

    private BranchOperatingStatusResponse closed(
            Schedule schedule, Optional<Opening> nextOpening, boolean acceptingOrders) {
        return new BranchOperatingStatusResponse(
                false,
                false,
                acceptingOrders,
                schedule.closed() || !hasInterval(schedule),
                schedule.open(),
                schedule.close(),
                nextOpening.map(Opening::at).orElse(null),
                null,
                schedule.reason());
    }

    private BranchOperatingStatusResponse unavailable(String reason, boolean acceptingOrders) {
        return new BranchOperatingStatusResponse(
                false, false, acceptingOrders, false, null, null, null, null, reason);
    }

    private BranchOperatingStatusResponse closedToday(
            String reason, Optional<Opening> nextOpening, boolean acceptingOrders) {
        return new BranchOperatingStatusResponse(
                false,
                false,
                acceptingOrders,
                true,
                null,
                null,
                nextOpening.map(Opening::at).orElse(null),
                null,
                reason);
    }

    private Optional<Opening> findNextOpening(UUID branchId, LocalDate date, LocalTime time) {
        for (int offset = 0; offset <= MAX_LOOKAHEAD_DAYS; offset++) {
            LocalDate candidateDate = date.plusDays(offset);
            Optional<Schedule> candidate = schedule(branchId, candidateDate);
            if (candidate.isEmpty() || !hasInterval(candidate.orElseThrow())) {
                continue;
            }
            if (offset == 0 && !opensLaterToday(candidate.orElseThrow(), time)) {
                continue;
            }
            Schedule candidateSchedule = candidate.orElseThrow();
            return Optional.of(new Opening(candidateDate, candidateSchedule.open()));
        }
        return Optional.empty();
    }

    private boolean opensLaterToday(Schedule schedule, LocalTime time) {
        return hasInterval(schedule) && time.isBefore(schedule.open());
    }

    private Optional<Schedule> schedule(UUID branchId, LocalDate date) {
        return specials.findByBranchIdAndSpecialDate(branchId, date)
                .map(
                        hour ->
                                new Schedule(
                                        hour.isClosed(),
                                        hour.getOpenTime(),
                                        hour.getCloseTime(),
                                        "SPECIAL_HOURS_CLOSED",
                                        true))
                .or(
                        () ->
                                hours.findByBranchIdAndDayOfWeek(
                                                branchId, (short) date.getDayOfWeek().getValue())
                                        .map(
                                                hour ->
                                                        new Schedule(
                                                                hour.isClosed(),
                                                                hour.getOpenTime(),
                                                                hour.getCloseTime(),
                                                                "BUSINESS_HOURS_CLOSED",
                                                                false)));
    }

    private boolean hasInterval(Schedule schedule) {
        return !schedule.closed()
                && schedule.open() != null
                && schedule.close() != null
                && !schedule.open().equals(schedule.close());
    }

    private boolean isWithinCurrentDay(Schedule schedule, LocalTime time) {
        if (!hasInterval(schedule)) {
            return false;
        }
        return schedule.close().isAfter(schedule.open())
                ? !time.isBefore(schedule.open()) && time.isBefore(schedule.close())
                : !time.isBefore(schedule.open());
    }

    private boolean isWithinPreviousDayCarryOver(Schedule schedule, LocalTime time) {
        return hasInterval(schedule)
                && schedule.close().isBefore(schedule.open())
                && time.isBefore(schedule.close());
    }

    private Optional<OffsetDateTime> closeAt(Schedule schedule, LocalDate scheduleDate) {
        if (!hasInterval(schedule)) {
            return Optional.empty();
        }
        LocalDate closeDate =
                schedule.close().isBefore(schedule.open())
                        ? scheduleDate.plusDays(1)
                        : scheduleDate;
        return Optional.of(
                ZonedDateTime.of(closeDate, schedule.close(), BUSINESS_ZONE).toOffsetDateTime());
    }

    private record Schedule(
            boolean closed, LocalTime open, LocalTime close, String reason, boolean special) {}

    private record Opening(LocalDate date, LocalTime time) {
        private OffsetDateTime at() {
            return ZonedDateTime.of(date, time, BUSINESS_ZONE).toOffsetDateTime();
        }
    }
}
