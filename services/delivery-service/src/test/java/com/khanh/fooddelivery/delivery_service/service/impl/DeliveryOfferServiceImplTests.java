package com.khanh.fooddelivery.delivery_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.delivery_service.client.DriverServiceClient;
import com.khanh.fooddelivery.delivery_service.client.OrderServiceClient;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
import com.khanh.fooddelivery.delivery_service.mapper.DeliveryMapper;
import com.khanh.fooddelivery.delivery_service.model.Delivery;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOffer;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOfferStatus;
import com.khanh.fooddelivery.delivery_service.model.DeliveryStatus;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryOfferRepository;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryRepository;
import com.khanh.fooddelivery.delivery_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.delivery_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.delivery_service.service.DeliveryMatchingService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class DeliveryOfferServiceImplTests {

    private static final UUID DELIVERY_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID DRIVER_A = UUID.fromString("00000000-0000-0000-0000-00000000010a");
    private static final UUID DRIVER_B = UUID.fromString("00000000-0000-0000-0000-00000000010b");

    @Mock
    private DeliveryRepository deliveries;
    @Mock
    private DeliveryOfferRepository offers;
    @Mock
    private CurrentUserProvider users;
    @Mock
    private CurrentBearerTokenProvider bearer;
    @Mock
    private OrderServiceClient orders;
    @Mock
    private DriverServiceClient drivers;
    @Mock
    private DeliveryMatchingService matching;
    @Mock
    private Jwt jwt;

    private DeliveryOfferServiceImpl offerService;
    private Delivery delivery;
    private DeliveryOffer offer;

    @BeforeEach
    void setUp() {
        offerService = new DeliveryOfferServiceImpl(
                deliveries,
                offers,
                users,
                bearer,
                orders,
                drivers,
                matching,
                Mappers.getMapper(DeliveryMapper.class)
        );
        delivery = delivery(DeliveryStatus.MATCHING);
        offer = offer(DeliveryOfferStatus.PENDING, Instant.now().plusSeconds(30));
        lenient().when(users.getCurrentUserId(jwt)).thenReturn(DRIVER_A);
        lenient().when(bearer.getBearerToken()).thenReturn("Bearer test-token");
        lenient().when(deliveries.findByIdForUpdate(DELIVERY_ID)).thenReturn(Optional.of(delivery));
        lenient().when(offers.findByDeliveryIdAndDriverIdAndStatus(
                DELIVERY_ID,
                DRIVER_A,
                DeliveryOfferStatus.PENDING
        )).thenReturn(Optional.of(offer));
    }

    @Test
    void accept_assigns_delivery_and_converts_offer_atomically_in_service_flow() {
        var response = offerService.accept(jwt, DELIVERY_ID);

        assertThat(response.status()).isEqualTo(DeliveryStatus.ASSIGNED);
        assertThat(response.driverId()).isEqualTo(DRIVER_A);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
        assertThat(delivery.getDriverId()).isEqualTo(DRIVER_A);
        assertThat(offer.getStatus()).isEqualTo(DeliveryOfferStatus.ACCEPTED);
        assertThat(offer.getRespondedAt()).isNotNull();
        verify(drivers).acceptOffer("Bearer test-token", DRIVER_A, DELIVERY_ID);
        verify(orders).assigned("Bearer test-token", ORDER_ID);
    }

    @Test
    void current_offer_returns_only_a_fresh_offer_for_the_authenticated_driver() {
        when(offers.findCurrentByDriverId(
                eq(DRIVER_A),
                eq(DeliveryOfferStatus.PENDING),
                any(Instant.class)
        )).thenReturn(List.of(offer));
        when(deliveries.findById(DELIVERY_ID)).thenReturn(Optional.of(delivery));

        var current = offerService.currentOffer(jwt);

        assertThat(current).isPresent();
        assertThat(current.orElseThrow().offerId()).isEqualTo(offer.getId());
        assertThat(current.orElseThrow().deliveryId()).isEqualTo(DELIVERY_ID);
        verify(offers).findCurrentByDriverId(eq(DRIVER_A), eq(DeliveryOfferStatus.PENDING), any(Instant.class));
    }

    @Test
    void current_active_delivery_exposes_only_assigned_or_picked_up_state() {
        delivery.setStatus(DeliveryStatus.PICKED_UP);
        delivery.setDriverId(DRIVER_A);
        when(deliveries.findByDriverIdAndStatusInOrderByUpdatedAtDesc(
                eq(DRIVER_A),
                eq(List.of(DeliveryStatus.ASSIGNED, DeliveryStatus.PICKED_UP))
        )).thenReturn(List.of(delivery));

        var current = offerService.currentActiveDelivery(jwt);

        assertThat(current).isPresent();
        assertThat(current.orElseThrow().status()).isEqualTo(DeliveryStatus.PICKED_UP);
    }

    @Test
    void wrong_driver_cannot_accept_another_drivers_offer() {
        when(users.getCurrentUserId(jwt)).thenReturn(DRIVER_B);
        when(offers.findByDeliveryIdAndDriverIdAndStatus(
                DELIVERY_ID,
                DRIVER_B,
                DeliveryOfferStatus.PENDING
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerService.accept(jwt, DELIVERY_ID))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.DELIVERY_CONFLICT);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.MATCHING);
        assertThat(offer.getStatus()).isEqualTo(DeliveryOfferStatus.PENDING);
        verify(drivers, never()).acceptOffer(anyString(), any(UUID.class), any(UUID.class));
        verify(orders, never()).assigned(anyString(), any(UUID.class));
    }

    @Test
    void second_accept_cannot_reassign_an_already_accepted_offer() {
        when(offers.findByDeliveryIdAndDriverIdAndStatus(
                DELIVERY_ID,
                DRIVER_A,
                DeliveryOfferStatus.PENDING
        )).thenAnswer(invocation -> offer.getStatus() == DeliveryOfferStatus.PENDING
                ? Optional.of(offer)
                : Optional.empty());

        offerService.accept(jwt, DELIVERY_ID);

        assertThatThrownBy(() -> offerService.accept(jwt, DELIVERY_ID))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.DELIVERY_CONFLICT);

        verify(drivers).acceptOffer("Bearer test-token", DRIVER_A, DELIVERY_ID);
        verify(orders).assigned("Bearer test-token", ORDER_ID);
        assertThat(delivery.getDriverId()).isEqualTo(DRIVER_A);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
    }

    @Test
    void assigned_delivery_cannot_be_taken_by_a_stale_pending_offer() {
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setDriverId(DRIVER_A);

        assertThatThrownBy(() -> offerService.accept(jwt, DELIVERY_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Delivery offer is no longer active");

        assertThat(offer.getStatus()).isEqualTo(DeliveryOfferStatus.PENDING);
        verify(drivers, never()).acceptOffer(anyString(), any(UUID.class), any(UUID.class));
        verify(orders, never()).assigned(anyString(), any(UUID.class));
    }

    @Test
    void reject_releases_candidate_and_requests_the_next_nearest_driver() {
        offerService.reject(jwt, DELIVERY_ID);

        assertThat(offer.getStatus()).isEqualTo(DeliveryOfferStatus.REJECTED);
        assertThat(offer.getRespondedAt()).isNotNull();
        verify(drivers).releaseOffer("Bearer test-token", DRIVER_A, DELIVERY_ID);
        verify(matching).offerNext(delivery);
    }

    @Test
    void expired_offer_releases_candidate_and_requests_the_next_nearest_driver() {
        offer.setExpiresAt(Instant.now().minusSeconds(1));
        when(offers.findExpired(any(Instant.class))).thenReturn(List.of(offer));

        offerService.expireOffers();

        assertThat(offer.getStatus()).isEqualTo(DeliveryOfferStatus.EXPIRED);
        assertThat(offer.getRespondedAt()).isNotNull();
        verify(drivers).releaseOffer("Bearer test-token", DRIVER_A, DELIVERY_ID);
        verify(matching).offerNext(delivery);
    }

    private Delivery delivery(DeliveryStatus status) {
        Delivery value = new Delivery();
        value.setId(DELIVERY_ID);
        value.setOrderId(ORDER_ID);
        value.setStatus(status);
        return value;
    }

    private DeliveryOffer offer(DeliveryOfferStatus status, Instant expiresAt) {
        DeliveryOffer value = new DeliveryOffer();
        value.setId(UUID.randomUUID());
        value.setDeliveryId(DELIVERY_ID);
        value.setDriverId(DRIVER_A);
        value.setStatus(status);
        value.setExpiresAt(expiresAt);
        return value;
    }
}
