package com.khanh.fooddelivery.delivery_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.delivery_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.delivery_service.client.UserServiceClient;
import com.khanh.fooddelivery.delivery_service.client.dto.response.InternalUserAddressResponse;
import com.khanh.fooddelivery.delivery_service.client.dto.response.RestaurantBranchOrderingContextResponse;
import com.khanh.fooddelivery.delivery_service.common.response.ApiResponse;
import com.khanh.fooddelivery.delivery_service.config.DeliveryQuoteProperties;
import com.khanh.fooddelivery.delivery_service.dto.request.CreateDeliveryQuoteRequest;
import com.khanh.fooddelivery.delivery_service.dto.request.DeliveryTargetRequest;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
import com.khanh.fooddelivery.delivery_service.mapper.DeliveryQuoteMapper;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryQuoteRepository;
import com.khanh.fooddelivery.delivery_service.repository.CheckoutTemporaryLocationRepository;
import com.khanh.fooddelivery.delivery_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.delivery_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.delivery_service.service.RoutingProvider;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class DeliveryQuoteServiceImplTests {
    private final UUID ownerId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID addressId = UUID.randomUUID();
    @Mock private RestaurantServiceClient restaurants;
    @Mock private UserServiceClient users;
    @Mock private CurrentUserProvider currentUser;
    @Mock private CurrentBearerTokenProvider bearer;
    @Mock private RoutingProvider routing;
    @Mock private DeliveryQuoteRepository quotes;
    @Mock private CheckoutTemporaryLocationRepository checkoutLocations;
    @Mock private Jwt jwt;
    private DeliveryQuoteServiceImpl service;

    @BeforeEach
    void setUp() {
        DeliveryQuoteProperties properties = new DeliveryQuoteProperties();
        properties.setTtl(Duration.ofMinutes(5));
        properties.setBaseFee(BigDecimal.valueOf(15000));
        properties.setIncludedDistanceMeters(3000);
        properties.setFeePerKm(BigDecimal.valueOf(5000));
        properties.setMaximumServiceDistanceMeters(10000);
        properties.setCurrency("VND");
        properties.setPricingPolicyVersion("test-v1");
        DeliveryQuoteMapper quoteMapper = Mappers.getMapper(DeliveryQuoteMapper.class);
        service = new DeliveryQuoteServiceImpl(
                restaurants,
                users,
                currentUser,
                bearer,
                routing,
                quotes,
                checkoutLocations,
                properties,
                quoteMapper
        );
        when(currentUser.getCurrentUserId(jwt)).thenReturn(ownerId);
        when(bearer.getBearerToken()).thenReturn("Bearer token");
        when(restaurants.getOrderingContext(anyString(), any())).thenReturn(success(new RestaurantBranchOrderingContextResponse(
                UUID.randomUUID(), "Restaurant", true, branchId, "Branch", true, true,
                BigDecimal.valueOf(10.7), BigDecimal.valueOf(106.7))));
        lenient().when(users.getOwnedAddress(anyString(), any())).thenReturn(success(
                new InternalUserAddressResponse(addressId, BigDecimal.valueOf(10.8), BigDecimal.valueOf(106.8))));
    }

    @Test
    void persistsBoundQuoteAndRoundsFeeFromRoadDistance() {
        when(routing.calculateRoute(any(), any(), any(), any())).thenReturn(new RoutingProvider.Route(4500, 721));

        var response = service.createQuote(jwt, new CreateDeliveryQuoteRequest(branchId, addressId));

        assertThat(response.deliveryFee()).isEqualByComparingTo("22500");
        assertThat(response.distanceMeters()).isEqualTo(4500);
        assertThat(response.estimatedDurationMinutes()).isEqualTo(13);
        assertThat(response.expiresAt()).isAfter(response.calculatedAt());
        ArgumentCaptor<com.khanh.fooddelivery.delivery_service.model.DeliveryQuote> quote = ArgumentCaptor.forClass(com.khanh.fooddelivery.delivery_service.model.DeliveryQuote.class);
        verify(quotes).save(quote.capture(), org.mockito.ArgumentMatchers.eq(Duration.ofMinutes(5)));
        assertThat(quote.getValue().ownerUserId()).isEqualTo(ownerId);
        assertThat(quote.getValue().branchId()).isEqualTo(branchId);
        assertThat(quote.getValue().addressId()).isEqualTo(addressId);
    }

    @Test
    void temporaryCheckoutTargetUsesOnlyServerStoredLocationAndNeverMutatesOrLoadsSavedAddress() {
        UUID temporaryLocationId = UUID.randomUUID();
        when(checkoutLocations.findCurrent(ownerId, branchId)).thenReturn(Optional.of(
                new com.khanh.fooddelivery.delivery_service.model.CheckoutTemporaryLocation(
                        temporaryLocationId, ownerId, branchId, "Temporary", "Temporary", null, null, null,
                        BigDecimal.valueOf(10.8), BigDecimal.valueOf(106.8), Instant.now(), Instant.now(), Instant.now().plusSeconds(2700))));
        when(routing.calculateRoute(any(), any(), any(), any())).thenReturn(new RoutingProvider.Route(3000, 180));

        var response = service.createQuote(jwt, new CreateDeliveryQuoteRequest(branchId,
                new DeliveryTargetRequest(com.khanh.fooddelivery.delivery_service.model.DeliveryTargetType.TEMPORARY_LOCATION, null, temporaryLocationId)));

        assertThat(response.deliveryFee()).isEqualByComparingTo("15000");
        verify(users, never()).getOwnedAddress(anyString(), any());
    }

    @Test
    void rejectsAddressWithoutCoordinatesBeforeRouting() {
        when(users.getOwnedAddress(anyString(), any())).thenReturn(success(new InternalUserAddressResponse(addressId, null, null)));

        assertError(() -> service.createQuote(jwt, new CreateDeliveryQuoteRequest(branchId, addressId)), ErrorCode.ADDRESS_COORDINATES_MISSING);
        verify(routing, never()).calculateRoute(any(), any(), any(), any());
        verify(quotes, never()).save(any(), any());
    }

    @Test
    void rejectsRouteBeyondConfiguredServiceDistanceWithoutPersistingQuote() {
        when(routing.calculateRoute(any(), any(), any(), any())).thenReturn(new RoutingProvider.Route(10001, 1000));

        assertError(() -> service.createQuote(jwt, new CreateDeliveryQuoteRequest(branchId, addressId)), ErrorCode.DELIVERY_NOT_SERVICEABLE);
        verify(quotes, never()).save(any(), any());
    }

    private static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "SUCCESS", "", data, Instant.now());
    }

    private void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, ErrorCode expected) {
        assertThatThrownBy(action).isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode()).isEqualTo(expected);
    }
}
