package com.khanh.fooddelivery.restaurant_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.restaurant_service.dto.response.BranchOperatingStatusResponse;
import com.khanh.fooddelivery.restaurant_service.entity.BranchBusinessHour;
import com.khanh.fooddelivery.restaurant_service.entity.BranchSpecialHour;
import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantBranchStatus;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantStatus;
import com.khanh.fooddelivery.restaurant_service.repository.BranchBusinessHourRepository;
import com.khanh.fooddelivery.restaurant_service.repository.BranchSpecialHourRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBranchRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BranchOperatingStatusServiceImplTests {
    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final LocalDate FRIDAY = LocalDate.of(2026, 8, 21);
    private static final ZoneOffset VIETNAM_OFFSET = ZoneOffset.ofHours(7);

    @Mock private RestaurantBranchRepository branches;
    @Mock private BranchSpecialHourRepository specials;
    @Mock private BranchBusinessHourRepository hours;

    private BranchOperatingStatusServiceImpl service;
    private Map<Short, BranchBusinessHour> weekly;
    private Map<LocalDate, BranchSpecialHour> specialByDate;

    @BeforeEach
    void setUp() {
        service = new BranchOperatingStatusServiceImpl(branches, specials, hours);
        weekly = new HashMap<>();
        specialByDate = new HashMap<>();
        when(branches.findById(BRANCH_ID)).thenReturn(Optional.of(activeBranch()));
        when(specials.findByBranchIdAndSpecialDate(any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(specialByDate.get(invocation.getArgument(1))));
        lenient().when(hours.findByBranchIdAndDayOfWeek(any(), anyShort()))
                .thenAnswer(invocation -> Optional.ofNullable(weekly.get(invocation.getArgument(1))));
    }

    @Test
    void openAtVietnamTimeUsesBusinessTimezoneEvenWhenCallerTimestampIsUtc() {
        weekly.put((short) 5, hour((short) 5, LocalTime.of(8, 0), LocalTime.of(22, 0), false));

        BranchOperatingStatusResponse response =
                service.getOperatingStatus(
                        BRANCH_ID,
                        ZonedDateTime.of(
                                LocalDateTime.of(2026, 8, 21, 6, 20), ZoneOffset.UTC));

        assertThat(response.open()).isTrue();
        assertThat(response.withinBusinessHours()).isTrue();
        assertThat(response.closeAt())
                .isEqualTo(OffsetDateTime.of(FRIDAY, LocalTime.of(22, 0), VIETNAM_OFFSET));
    }

    @Test
    void beforeOpeningReturnsOpeningToday() {
        weekly.put((short) 5, hour((short) 5, LocalTime.of(8, 0), LocalTime.of(22, 0), false));

        BranchOperatingStatusResponse response =
                service.getOperatingStatus(BRANCH_ID, atVietnam(6, 0));

        assertThat(response.open()).isFalse();
        assertThat(response.closedToday()).isFalse();
        assertThat(response.nextOpenAt())
                .isEqualTo(OffsetDateTime.of(FRIDAY, LocalTime.of(8, 0), VIETNAM_OFFSET));
    }

    @Test
    void afterClosingReturnsOpeningOnNextDay() {
        weekly.put((short) 5, hour((short) 5, LocalTime.of(8, 0), LocalTime.of(22, 0), false));
        weekly.put((short) 6, hour((short) 6, LocalTime.of(8, 0), LocalTime.of(22, 0), false));

        BranchOperatingStatusResponse response =
                service.getOperatingStatus(BRANCH_ID, atVietnam(23, 0));

        assertThat(response.open()).isFalse();
        assertThat(response.nextOpenAt())
                .isEqualTo(
                        OffsetDateTime.of(
                                FRIDAY.plusDays(1), LocalTime.of(8, 0), VIETNAM_OFFSET));
    }

    @Test
    void closedDayFindsTheNextConfiguredOpening() {
        weekly.put((short) 5, hour((short) 5, null, null, true));
        weekly.put((short) 6, hour((short) 6, LocalTime.of(9, 0), LocalTime.of(17, 0), false));

        BranchOperatingStatusResponse response =
                service.getOperatingStatus(BRANCH_ID, atVietnam(10, 0));

        assertThat(response.open()).isFalse();
        assertThat(response.closedToday()).isTrue();
        assertThat(response.nextOpenAt())
                .isEqualTo(
                        OffsetDateTime.of(
                                FRIDAY.plusDays(1), LocalTime.of(9, 0), VIETNAM_OFFSET));
    }

    @Test
    void specialHoursOverrideWeeklyHours() {
        weekly.put((short) 5, hour((short) 5, LocalTime.of(8, 0), LocalTime.of(22, 0), false));
        specialByDate.put(
                FRIDAY,
                special(FRIDAY, LocalTime.of(10, 0), LocalTime.of(14, 0), false));

        BranchOperatingStatusResponse response =
                service.getOperatingStatus(BRANCH_ID, atVietnam(9, 0));

        assertThat(response.open()).isFalse();
        assertThat(response.nextOpenAt())
                .isEqualTo(OffsetDateTime.of(FRIDAY, LocalTime.of(10, 0), VIETNAM_OFFSET));

        response = service.getOperatingStatus(BRANCH_ID, atVietnam(12, 0));

        assertThat(response.open()).isTrue();
        assertThat(response.closeAt())
                .isEqualTo(OffsetDateTime.of(FRIDAY, LocalTime.of(14, 0), VIETNAM_OFFSET));
    }

    @Test
    void overnightHoursRemainOpenAfterMidnight() {
        weekly.put((short) 5, hour((short) 5, LocalTime.of(18, 0), LocalTime.of(2, 0), false));

        BranchOperatingStatusResponse response =
                service.getOperatingStatus(BRANCH_ID, atVietnamOn(FRIDAY.plusDays(1), 1, 0));

        assertThat(response.open()).isTrue();
        assertThat(response.closeAt())
                .isEqualTo(
                        OffsetDateTime.of(
                                FRIDAY.plusDays(1), LocalTime.of(2, 0), VIETNAM_OFFSET));
    }

    private ZonedDateTime atVietnam(int hour, int minute) {
        return atVietnamOn(FRIDAY, hour, minute);
    }

    private ZonedDateTime atVietnamOn(LocalDate date, int hour, int minute) {
        return ZonedDateTime.of(date, LocalTime.of(hour, minute), BranchOperatingStatusServiceImpl.BUSINESS_ZONE);
    }

    private RestaurantBranch activeBranch() {
        Restaurant restaurant = new Restaurant();
        restaurant.setStatus(RestaurantStatus.ACTIVE);

        RestaurantBranch branch = new RestaurantBranch();
        branch.setRestaurant(restaurant);
        branch.setStatus(RestaurantBranchStatus.ACTIVE);
        branch.setAcceptingOrders(true);
        return branch;
    }

    private BranchBusinessHour hour(
            short day, LocalTime open, LocalTime close, boolean closed) {
        BranchBusinessHour hour = new BranchBusinessHour();
        hour.setDayOfWeek(day);
        hour.setOpenTime(open);
        hour.setCloseTime(close);
        hour.setClosed(closed);
        return hour;
    }

    private BranchSpecialHour special(
            LocalDate date, LocalTime open, LocalTime close, boolean closed) {
        BranchSpecialHour special = new BranchSpecialHour();
        special.setSpecialDate(date);
        special.setOpenTime(open);
        special.setCloseTime(close);
        special.setClosed(closed);
        return special;
    }
}
