package com.khanh.fooddelivery.delivery_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.khanh.fooddelivery.delivery_service.client.DriverServiceClient;
import com.khanh.fooddelivery.delivery_service.client.OrderServiceClient;
import com.khanh.fooddelivery.delivery_service.client.TrackingServiceClient;
import com.khanh.fooddelivery.delivery_service.client.dto.response.NearestDriverResponse;
import com.khanh.fooddelivery.delivery_service.model.Delivery;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOffer;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOfferStatus;
import com.khanh.fooddelivery.delivery_service.model.DeliveryStatus;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryOfferRepository;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryRepository;
import com.khanh.fooddelivery.delivery_service.security.CurrentBearerTokenProvider;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DriverDispatchServiceImplTests {

    private static final UUID DELIVERY_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID DRIVER_A = UUID.randomUUID();

    @Mock private DeliveryRepository deliveries;
    @Mock private DeliveryOfferRepository offers;
    @Mock private CurrentBearerTokenProvider bearer;
    @Mock private OrderServiceClient orders;
    @Mock private DriverServiceClient drivers;
    @Mock private TrackingServiceClient tracking;
    @Mock private ApplicationEventPublisher events;

    private DriverDispatchServiceImpl dispatch;
    private Delivery delivery;

    @BeforeEach
    void setUp() {
        dispatch = new DriverDispatchServiceImpl(deliveries, offers, bearer, orders, drivers, tracking, events);
        ReflectionTestUtils.setField(dispatch, "offerTtl", Duration.ofSeconds(45));
        ReflectionTestUtils.setField(dispatch, "searchRadii", List.of(2000d, 4000d, 6000d, 8000d));
        ReflectionTestUtils.setField(dispatch, "candidatesPerRadius", 20L);
        ReflectionTestUtils.setField(dispatch, "retryDelay", Duration.ofSeconds(15));
        ReflectionTestUtils.setField(dispatch, "dispatchMaxDuration", Duration.ofMinutes(10));
        ReflectionTestUtils.setField(dispatch, "internalApiKey", "internal-test-key");
        lenient().when(bearer.getBearerToken()).thenReturn("Bearer test-token");
        delivery = delivery();
        lenient().when(deliveries.findByIdForUpdate(DELIVERY_ID)).thenReturn(Optional.of(delivery));
        lenient().when(offers.existsByDeliveryIdAndStatus(DELIVERY_ID, DeliveryOfferStatus.PENDING)).thenReturn(false);
        lenient().when(offers.findByDeliveryIdOrderByOfferedAtAsc(DELIVERY_ID)).thenReturn(List.of());
        lenient().when(drivers.available("Bearer test-token")).thenReturn(List.of(DRIVER_A));
        lenient().when(tracking.nearest(anyString(), any(), any(), anyDouble(), anyLong()))
                .thenReturn(List.of(new NearestDriverResponse(DRIVER_A, 500L, Instant.now())));
        lenient().when(offers.save(any(DeliveryOffer.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void noCandidateKeepsDeliveryMatchingAndSchedulesRetry() {
        when(drivers.available("Bearer test-token")).thenReturn(List.of());

        dispatch.dispatch(DELIVERY_ID);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.MATCHING);
        assertThat(delivery.getNextDispatchAt()).isAfter(Instant.now().minusSeconds(1));
        assertThat(delivery.getDispatchAttemptCount()).isEqualTo(1);
        verify(orders, never()).matchingFailed(anyString(), any(UUID.class));
        verify(drivers, never()).reserveOffer(anyString(), any(UUID.class), any(UUID.class));
    }

    @Test
    void eligibleCandidateGetsExactlyOnePendingOffer() {
        dispatch.dispatch(DELIVERY_ID);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.MATCHING);
        assertThat(delivery.getNextDispatchAt()).isNull();
        verify(drivers).reserveOffer("Bearer test-token", DRIVER_A, DELIVERY_ID);
        verify(offers).save(any(DeliveryOffer.class));
    }

    @Test
    void deadlineMarksDeliveryFailedAndNotifiesOrderOnce() {
        delivery.setDispatchDeadlineAt(Instant.now().minusSeconds(1));

        dispatch.dispatch(DELIVERY_ID);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.MATCH_FAILED);
        assertThat(delivery.getNextDispatchAt()).isNull();
        verify(orders).matchingFailed("Bearer test-token", ORDER_ID);
        verify(drivers, never()).available(anyString());
    }

    @Test
    void pendingOfferPreventsAnotherDispatch() {
        when(offers.existsByDeliveryIdAndStatus(DELIVERY_ID, DeliveryOfferStatus.PENDING)).thenReturn(true);

        dispatch.dispatch(DELIVERY_ID);

        verify(drivers, never()).available(anyString());
        verify(tracking, never()).nearest(anyString(), any(), any(), anyDouble(), anyLong());
    }

    @Test
    void transientTrackingFailureKeepsDeliveryMatchingForRetry() {
        when(tracking.nearest(anyString(), any(), any(), anyDouble(), anyLong()))
                .thenThrow(new IllegalStateException("tracking unavailable"));

        dispatch.dispatch(DELIVERY_ID);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.MATCHING);
        assertThat(delivery.getNextDispatchAt()).isAfter(Instant.now().minusSeconds(1));
        verify(orders, never()).matchingFailed(anyString(), any(UUID.class));
        verify(drivers, never()).reserveOffer(anyString(), any(UUID.class), any(UUID.class));
    }

    @Test
    void previouslyRejectedDriverIsSkippedOnNextAttempt() {
        DeliveryOffer rejected = new DeliveryOffer();
        rejected.setDriverId(DRIVER_A);
        rejected.setStatus(DeliveryOfferStatus.REJECTED);
        when(offers.findByDeliveryIdOrderByOfferedAtAsc(DELIVERY_ID)).thenReturn(List.of(rejected));
        when(tracking.nearest(anyString(), any(), any(), anyDouble(), anyLong()))
                .thenReturn(List.of(new NearestDriverResponse(DRIVER_A, 500L, Instant.now())));

        dispatch.dispatch(DELIVERY_ID);

        verify(drivers, never()).reserveOffer(anyString(), eq(DRIVER_A), eq(DELIVERY_ID));
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.MATCHING);
    }

    @Test
    void failedOfferPersistenceReleasesExternalReservation() {
        when(offers.save(any(DeliveryOffer.class))).thenThrow(new IllegalStateException("database unavailable"));

        dispatch.dispatch(DELIVERY_ID);

        verify(drivers).reserveOffer("Bearer test-token", DRIVER_A, DELIVERY_ID);
        verify(drivers).releaseOffer("Bearer test-token", DRIVER_A, DELIVERY_ID);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.MATCHING);
    }

    private Delivery delivery() {
        Delivery value = new Delivery();
        value.setId(DELIVERY_ID);
        value.setOrderId(ORDER_ID);
        value.setStatus(DeliveryStatus.MATCHING);
        value.setPickupLatitude(new BigDecimal("21.0300"));
        value.setPickupLongitude(new BigDecimal("105.8500"));
        value.setMatchingStartedAt(Instant.now());
        value.setNextDispatchAt(Instant.now().minusSeconds(1));
        value.setDispatchDeadlineAt(Instant.now().plusSeconds(600));
        value.setDispatchAttemptCount(0);
        return value;
    }
}
