package com.khanh.fooddelivery.delivery_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.delivery_service.client.DriverServiceClient;
import com.khanh.fooddelivery.delivery_service.client.OrderServiceClient;
import com.khanh.fooddelivery.delivery_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.delivery_service.client.TrackingServiceClient;
import com.khanh.fooddelivery.delivery_service.client.dto.response.NearestDriverResponse;
import com.khanh.fooddelivery.delivery_service.client.dto.response.RestaurantBranchOrderingContextResponse;
import com.khanh.fooddelivery.delivery_service.common.response.ApiResponse;
import com.khanh.fooddelivery.delivery_service.dto.request.DeliveryMatchingRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryResponse;
import com.khanh.fooddelivery.delivery_service.mapper.DeliveryMapper;
import com.khanh.fooddelivery.delivery_service.model.Delivery;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOffer;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOfferStatus;
import com.khanh.fooddelivery.delivery_service.model.DeliveryStatus;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryOfferRepository;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryRepository;
import com.khanh.fooddelivery.delivery_service.security.CurrentBearerTokenProvider;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeliveryMatchingServiceImplTests {

    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RESTAURANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID BRANCH_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID DELIVERY_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID DRIVER_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID DRIVER_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID DRIVER_C = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    @Mock
    private DeliveryRepository deliveries;
    @Mock
    private DeliveryOfferRepository offers;
    @Mock
    private CurrentBearerTokenProvider bearer;
    @Mock
    private OrderServiceClient orders;
    @Mock
    private DriverServiceClient drivers;
    @Mock
    private RestaurantServiceClient restaurants;
    @Mock
    private TrackingServiceClient tracking;
    @Mock
    private ApplicationEventPublisher events;

    private final DeliveryMapper mapper = Mappers.getMapper(DeliveryMapper.class);
    private final List<DeliveryOffer> offerRows = new ArrayList<>();
    private DeliveryMatchingServiceImpl matching;

    @BeforeEach
    void setUp() {
        matching = new DeliveryMatchingServiceImpl(
                deliveries,
                offers,
                bearer,
                orders,
                drivers,
                restaurants,
                tracking,
                mapper,
                events
        );
        ReflectionTestUtils.setField(matching, "offerTtl", java.time.Duration.ofSeconds(45));
        ReflectionTestUtils.setField(matching, "searchRadii", List.of(2000d, 4000d, 6000d, 8000d));
        ReflectionTestUtils.setField(matching, "candidatesPerRadius", 20L);
        ReflectionTestUtils.setField(matching, "internalApiKey", "internal-test-key");

        when(bearer.getBearerToken()).thenReturn("Bearer test-token");
        lenient().when(deliveries.save(any(Delivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(offers.findAll()).thenAnswer(invocation -> offerRows);
        lenient().when(offers.save(any(DeliveryOffer.class))).thenAnswer(invocation -> {
            DeliveryOffer offer = invocation.getArgument(0);
            offerRows.add(offer);
            return offer;
        });
        lenient().when(restaurants.getOrderingContext(any(), any(), any(UUID.class)))
                .thenReturn(ApiResponse.success(
                        "Ordering context",
                        new RestaurantBranchOrderingContextResponse(
                                RESTAURANT_ID,
                                "Restaurant",
                                true,
                                BRANCH_ID,
                                "Branch",
                                true,
                                true,
                                "120 Nguyen Trai",
                                "Ward 1",
                                "District 1",
                                "Ho Chi Minh City",
                                new BigDecimal("10.0000"),
                                new BigDecimal("20.0000")
                        )
                ));
    }

    @Test
    void offers_only_nearest_eligible_driver() {
        when(deliveries.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(drivers.available("Bearer test-token")).thenReturn(List.of(DRIVER_A, DRIVER_B, DRIVER_C));
        when(tracking.nearest(anyString(), any(), any(), anyDouble(), anyLong()))
                .thenReturn(nearest(DRIVER_A, DRIVER_B, DRIVER_C));

        DeliveryResponse response = matching.startMatching(request());

        assertThat(response.status()).isEqualTo(DeliveryStatus.MATCHING);
        assertThat(response.customerAddress()).isEqualTo("10 Delivery Street, Ward 2, District 2, Ho Chi Minh City");
        assertThat(response.customerAddressLabel()).isEqualTo("Công ty");
        assertThat(response.pickupAddress()).isEqualTo("120 Nguyen Trai, Ward 1, District 1, Ho Chi Minh City");
        assertThat(response.customerLatitude()).isEqualByComparingTo("10.1000");
        assertThat(response.customerLongitude()).isEqualByComparingTo("20.1000");
        assertThat(offerRows).singleElement()
                .extracting(DeliveryOffer::getDriverId)
                .isEqualTo(DRIVER_A);
        verify(drivers).reserveOffer("Bearer test-token", DRIVER_A, deliveryIdFromOffer());
        verify(drivers, never()).reserveOffer("Bearer test-token", DRIVER_B, deliveryIdFromOffer());
        verify(drivers, never()).reserveOffer("Bearer test-token", DRIVER_C, deliveryIdFromOffer());
        verify(tracking).nearest("Bearer test-token", new BigDecimal("10.0000"),
                new BigDecimal("20.0000"), 2000d, 20L);
    }

    @Test
    void rejected_nearest_driver_is_excluded_when_matching_is_requeried() {
        Delivery delivery = matchingDelivery();
        offerRows.add(offer(DRIVER_A, DeliveryOfferStatus.REJECTED));
        when(drivers.available("Bearer test-token")).thenReturn(List.of(DRIVER_A, DRIVER_B, DRIVER_C));
        when(tracking.nearest(anyString(), any(), any(), anyDouble(), anyLong()))
                .thenReturn(nearest(DRIVER_A, DRIVER_B, DRIVER_C));

        matching.offerNext(delivery);

        assertThat(offerRows).hasSize(2);
        assertThat(offerRows.get(1).getDriverId()).isEqualTo(DRIVER_B);
        verify(drivers).reserveOffer("Bearer test-token", DRIVER_B, DELIVERY_ID);
        verify(drivers, never()).reserveOffer("Bearer test-token", DRIVER_A, DELIVERY_ID);
    }

    @Test
    void expired_nearest_driver_is_excluded_when_matching_is_requeried() {
        Delivery delivery = matchingDelivery();
        offerRows.add(offer(DRIVER_A, DeliveryOfferStatus.EXPIRED));
        when(drivers.available("Bearer test-token")).thenReturn(List.of(DRIVER_A, DRIVER_B));
        when(tracking.nearest(anyString(), any(), any(), anyDouble(), anyLong()))
                .thenReturn(nearest(DRIVER_A, DRIVER_B));

        matching.offerNext(delivery);

        assertThat(offerRows).hasSize(2);
        assertThat(offerRows.get(1).getDriverId()).isEqualTo(DRIVER_B);
        verify(drivers).reserveOffer("Bearer test-token", DRIVER_B, DELIVERY_ID);
    }

    @Test
    void progressively_expands_radius_before_selecting_driver() {
        Delivery delivery = matchingDelivery();
        when(drivers.available("Bearer test-token")).thenReturn(List.of(DRIVER_A));
        when(tracking.nearest(anyString(), any(), any(), anyDouble(), anyLong()))
                .thenAnswer(invocation -> {
                    double radius = invocation.getArgument(3);
                    return radius == 2000d ? List.of() : nearest(DRIVER_A);
                });

        matching.offerNext(delivery);

        assertThat(offerRows).singleElement().extracting(DeliveryOffer::getDriverId).isEqualTo(DRIVER_A);
        verify(tracking).nearest("Bearer test-token", new BigDecimal("10.0000"),
                new BigDecimal("20.0000"), 2000d, 20L);
        verify(tracking).nearest("Bearer test-token", new BigDecimal("10.0000"),
                new BigDecimal("20.0000"), 4000d, 20L);
    }

    @Test
    void marks_delivery_match_failed_when_no_eligible_driver_is_found() {
        Delivery delivery = matchingDelivery();
        when(drivers.available("Bearer test-token")).thenReturn(List.of());
        when(tracking.nearest(anyString(), any(), any(), anyDouble(), anyLong())).thenReturn(List.of());

        matching.offerNext(delivery);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.MATCH_FAILED);
        assertThat(offerRows).isEmpty();
        verify(orders).matchingFailed("Bearer test-token", ORDER_ID);
        verify(drivers, never()).reserveOffer(anyString(), any(UUID.class), any(UUID.class));
        verify(tracking, times(4)).nearest(
                eq("Bearer test-token"),
                eq(new BigDecimal("10.0000")),
                eq(new BigDecimal("20.0000")),
                anyDouble(),
                eq(20L)
        );
    }

    @Test
    void keeps_legacy_delivery_valid_when_only_address_label_is_available() {
        when(deliveries.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(drivers.available("Bearer test-token")).thenReturn(List.of(DRIVER_A));
        when(tracking.nearest(anyString(), any(), any(), anyDouble(), anyLong()))
                .thenReturn(nearest(DRIVER_A));

        DeliveryMatchingRequest legacyRequest = new DeliveryMatchingRequest(
                ORDER_ID,
                RESTAURANT_ID,
                BRANCH_ID,
                CUSTOMER_ID,
                "Restaurant",
                "Branch",
                "Nhà",
                null,
                new BigDecimal("10.1000"),
                new BigDecimal("20.1000")
        );

        matching.startMatching(legacyRequest);

        assertThat(offerRows).singleElement();
        org.mockito.ArgumentCaptor<Delivery> deliveryCaptor =
                org.mockito.ArgumentCaptor.forClass(Delivery.class);
        verify(deliveries).save(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getValue().getCustomerAddress()).isEqualTo("Nhà");
    }

    @Test
    void tracking_failure_does_not_select_fallback_or_mark_delivery_failed() {
        Delivery delivery = matchingDelivery();
        when(drivers.available("Bearer test-token")).thenReturn(List.of(DRIVER_A));
        when(tracking.nearest(anyString(), any(), any(), anyDouble(), anyLong()))
                .thenThrow(new IllegalStateException("tracking unavailable"));

        assertThatThrownBy(() -> matching.offerNext(delivery))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("tracking unavailable");

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.MATCHING);
        assertThat(offerRows).isEmpty();
        verify(orders, never()).matchingFailed(anyString(), any(UUID.class));
        verify(drivers, never()).reserveOffer(anyString(), any(UUID.class), any(UUID.class));
    }

    @Test
    void offline_nearest_driver_is_skipped_by_driver_availability_snapshot() {
        assertOnlyDriverBIsSelected();
    }

    @Test
    void inactive_nearest_driver_is_skipped_by_driver_availability_snapshot() {
        assertOnlyDriverBIsSelected();
    }

    @Test
    void busy_nearest_driver_is_skipped_by_driver_availability_snapshot() {
        assertOnlyDriverBIsSelected();
    }

    @Test
    void pending_offer_conflict_nearest_driver_is_skipped_by_driver_availability_snapshot() {
        assertOnlyDriverBIsSelected();
    }

    @Test
    void already_rejected_driver_is_not_offered_again() {
        Delivery delivery = matchingDelivery();
        offerRows.add(offer(DRIVER_A, DeliveryOfferStatus.REJECTED));
        when(drivers.available("Bearer test-token")).thenReturn(List.of(DRIVER_A, DRIVER_B));
        when(tracking.nearest(anyString(), any(), any(), anyDouble(), anyLong()))
                .thenReturn(nearest(DRIVER_A, DRIVER_B));

        matching.offerNext(delivery);

        assertThat(offerRows).extracting(DeliveryOffer::getDriverId)
                .containsExactly(DRIVER_A, DRIVER_B);
    }

    private void assertOnlyDriverBIsSelected() {
        Delivery delivery = matchingDelivery();
        when(drivers.available("Bearer test-token")).thenReturn(List.of(DRIVER_B));
        when(tracking.nearest(anyString(), any(), any(), anyDouble(), anyLong()))
                .thenReturn(nearest(DRIVER_A, DRIVER_B));

        matching.offerNext(delivery);

        assertThat(offerRows).singleElement().extracting(DeliveryOffer::getDriverId).isEqualTo(DRIVER_B);
        verify(drivers).reserveOffer("Bearer test-token", DRIVER_B, DELIVERY_ID);
        verify(drivers, never()).reserveOffer("Bearer test-token", DRIVER_A, DELIVERY_ID);
    }

    private DeliveryMatchingRequest request() {
        return new DeliveryMatchingRequest(
                ORDER_ID,
                RESTAURANT_ID,
                BRANCH_ID,
                CUSTOMER_ID,
                "Restaurant",
                "Branch",
                "Công ty",
                "10 Delivery Street, Ward 2, District 2, Ho Chi Minh City",
                new BigDecimal("10.1000"),
                new BigDecimal("20.1000")
        );
    }

    private Delivery matchingDelivery() {
        Delivery delivery = new Delivery();
        delivery.setId(DELIVERY_ID);
        delivery.setOrderId(ORDER_ID);
        delivery.setRestaurantId(RESTAURANT_ID);
        delivery.setBranchId(BRANCH_ID);
        delivery.setCustomerId(CUSTOMER_ID);
        delivery.setRestaurantName("Restaurant");
        delivery.setBranchName("Branch");
        delivery.setCustomerAddress("10 Delivery Street");
        delivery.setPickupLatitude(new BigDecimal("10.0000"));
        delivery.setPickupLongitude(new BigDecimal("20.0000"));
        delivery.setStatus(DeliveryStatus.MATCHING);
        return delivery;
    }

    private List<NearestDriverResponse> nearest(UUID... driverIds) {
        List<NearestDriverResponse> result = new ArrayList<>();
        for (int index = 0; index < driverIds.length; index++) {
            long distance = index == 0 ? 500 : index == 1 ? 1000 : 2000;
            result.add(new NearestDriverResponse(driverIds[index], distance, java.time.Instant.now()));
        }
        return result;
    }

    private DeliveryOffer offer(UUID driverId, DeliveryOfferStatus status) {
        DeliveryOffer offer = new DeliveryOffer();
        offer.setId(UUID.randomUUID());
        offer.setDeliveryId(DELIVERY_ID);
        offer.setDriverId(driverId);
        offer.setStatus(status);
        offer.setExpiresAt(java.time.Instant.now().minusSeconds(1));
        return offer;
    }

    private UUID deliveryIdFromOffer() {
        return offerRows.isEmpty() ? DELIVERY_ID : offerRows.get(0).getDeliveryId();
    }
}
