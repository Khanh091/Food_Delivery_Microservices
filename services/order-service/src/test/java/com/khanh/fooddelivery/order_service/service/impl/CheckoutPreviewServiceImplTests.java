package com.khanh.fooddelivery.order_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.order_service.client.CartServiceClient;
import com.khanh.fooddelivery.order_service.client.CatalogServiceClient;
import com.khanh.fooddelivery.order_service.client.DeliveryServiceClient;
import com.khanh.fooddelivery.order_service.client.RemoteApiResponse;
import com.khanh.fooddelivery.order_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.order_service.client.UserServiceClient;
import com.khanh.fooddelivery.order_service.dto.request.CheckoutPreviewRequest;
import com.khanh.fooddelivery.order_service.exception.AppException;
import com.khanh.fooddelivery.order_service.exception.ErrorCode;
import com.khanh.fooddelivery.order_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.order_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.order_service.service.CheckoutPreviewFingerprint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class CheckoutPreviewServiceImplTests {
    private final UUID ownerId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID addressId = UUID.randomUUID();
    private final UUID cartItemId = UUID.randomUUID();
    private final UUID catalogItemId = UUID.randomUUID();

    @Mock private CartServiceClient carts;
    @Mock private UserServiceClient users;
    @Mock private RestaurantServiceClient restaurants;
    @Mock private CatalogServiceClient catalog;
    @Mock private DeliveryServiceClient delivery;
    @Mock private CurrentUserProvider currentUser;
    @Mock private CurrentBearerTokenProvider bearer;
    @Mock private Jwt jwt;

    private CheckoutPreviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CheckoutPreviewServiceImpl(
                carts, users, restaurants, catalog, delivery, currentUser, bearer, new CheckoutPreviewFingerprint());
        when(currentUser.getCurrentUserId(jwt)).thenReturn(ownerId);
        when(bearer.getBearerToken()).thenReturn("Bearer token");
        happyCart(3L);
        happyAddress();
        happyRestaurant(true);
        happyCatalog(BigDecimal.valueOf(55));
        happyDeliveryQuote();
    }

    @Test
    void happyPreviewUsesAuthoritativePriceAndOneBatchFeignCall() {
        var preview = service.preview(jwt, new CheckoutPreviewRequest(branchId, 3L, addressId));

        assertThat(preview.itemsSubtotal()).isEqualByComparingTo("110");
        assertThat(preview.deliveryFee()).isEqualByComparingTo("15000");
        assertThat(preview.totalAmount()).isEqualByComparingTo("15110");
        assertThat(preview.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(preview.canPlaceOrder()).isTrue();
        assertThat(preview.priceChanges()).singleElement()
                .satisfies(change -> assertThat(change.currentUnitPrice()).isEqualByComparingTo("55"));
        verify(catalog, times(1)).validateCheckoutItems(anyString(), any());
    }

    @Test
    void emptyCartIsRejected() {
        when(carts.getCurrentSnapshot(anyString(), any())).thenReturn(success(new CartServiceClient.InternalCartSnapshotResponse(
                ownerId, null, null, null, List.of(), 0L, null, null)));

        assertError(() -> service.preview(jwt, new CheckoutPreviewRequest(branchId, 1L, addressId)), ErrorCode.CART_EMPTY);
    }

    @Test
    void staleCartVersionIsRejectedBeforeDownstreamValidation() {
        assertError(() -> service.preview(jwt, new CheckoutPreviewRequest(branchId, 2L, addressId)), ErrorCode.CART_VERSION_CONFLICT);
    }

    @Test
    void branchNotAcceptingOrdersIsRejected() {
        happyRestaurant(false);

        assertError(() -> service.preview(jwt, new CheckoutPreviewRequest(branchId, 3L, addressId)), ErrorCode.BRANCH_NOT_ACCEPTING_ORDERS);
    }

    @Test
    void catalogUnavailableIsMappedWithoutReturningStaleCartPrices() {
        when(catalog.validateCheckoutItems(anyString(), any())).thenReturn(null);

        assertError(() -> service.preview(jwt, new CheckoutPreviewRequest(branchId, 3L, addressId)), ErrorCode.CATALOG_SERVICE_UNAVAILABLE);
    }

    @Test
    void addressWithoutCoordinatesReturnsPartialLocationRequiredPreviewWithoutCallingDelivery() {
        when(users.getOwnedAddress(anyString(), any())).thenReturn(success(new UserServiceClient.InternalUserAddressResponse(
                addressId, "HOME", null, "Home", "Customer", "84912345678", "1 Nguyen Trai", null,
                "District 1", "Ho Chi Minh City", null, null, null, null, null, null, 2L)));

        var preview = service.preview(jwt, new CheckoutPreviewRequest(branchId, 3L, addressId));

        assertThat(preview.deliveryQuoteStatus()).isEqualTo(com.khanh.fooddelivery.order_service.dto.response.DeliveryQuoteStatus.LOCATION_REQUIRED);
        assertThat(preview.items()).hasSize(1);
        assertThat(preview.itemsSubtotal()).isEqualByComparingTo("110");
        assertThat(preview.deliveryFee()).isNull();
        assertThat(preview.totalAmount()).isNull();
        assertThat(preview.canPlaceOrder()).isFalse();
        verify(delivery, never()).createQuote(anyString(), any());
    }

    @Test
    void unavailableDeliveryProviderReturnsPartialPreview() {
        when(delivery.createQuote(anyString(), any())).thenReturn(null);

        var preview = service.preview(jwt, new CheckoutPreviewRequest(branchId, 3L, addressId));

        assertThat(preview.deliveryQuoteStatus()).isEqualTo(com.khanh.fooddelivery.order_service.dto.response.DeliveryQuoteStatus.TEMPORARILY_UNAVAILABLE);
        assertThat(preview.items()).hasSize(1);
        assertThat(preview.itemsSubtotal()).isEqualByComparingTo("110");
        assertThat(preview.canPlaceOrder()).isFalse();
    }

    @Test
    void notServiceableDeliveryKeepsAuthoritativePreviewData() {
        FeignException conflict = org.mockito.Mockito.mock(FeignException.class);
        when(conflict.status()).thenReturn(409);
        when(delivery.createQuote(anyString(), any())).thenThrow(conflict);

        var preview = service.preview(jwt, new CheckoutPreviewRequest(branchId, 3L, addressId));

        assertThat(preview.deliveryQuoteStatus()).isEqualTo(com.khanh.fooddelivery.order_service.dto.response.DeliveryQuoteStatus.NOT_SERVICEABLE);
        assertThat(preview.items()).hasSize(1);
        assertThat(preview.itemsSubtotal()).isEqualByComparingTo("110");
        assertThat(preview.deliveryFee()).isNull();
        assertThat(preview.canPlaceOrder()).isFalse();
    }

    @Test
    void fingerprintIsDeterministicAndChangesWhenAuthoritativePriceChanges() {
        var first = service.preview(jwt, new CheckoutPreviewRequest(branchId, 3L, addressId));
        var second = service.preview(jwt, new CheckoutPreviewRequest(branchId, 3L, addressId));
        happyCatalog(BigDecimal.valueOf(60));
        var changed = service.preview(jwt, new CheckoutPreviewRequest(branchId, 3L, addressId));

        assertThat(first.previewFingerprint()).isEqualTo(second.previewFingerprint());
        assertThat(changed.previewFingerprint()).isNotEqualTo(first.previewFingerprint());
    }

    private void happyCart(long version) {
        CartServiceClient.InternalSelectedOptionSnapshotResponse option =
                new CartServiceClient.InternalSelectedOptionSnapshotResponse(
                        UUID.randomUUID(), UUID.randomUUID(), "Size", "Large", BigDecimal.TEN);
        CartServiceClient.InternalCartItemSnapshotResponse item =
                new CartServiceClient.InternalCartItemSnapshotResponse(
                        cartItemId, catalogItemId, UUID.randomUUID(), 2, "no onion", List.of(option), "Burger", null,
                        BigDecimal.valueOf(40), BigDecimal.TEN, BigDecimal.valueOf(50), null);
        when(carts.getCurrentSnapshot(anyString(), any())).thenReturn(success(new CartServiceClient.InternalCartSnapshotResponse(
                ownerId, restaurantId, branchId, "VND", List.of(item), version, Instant.parse("2026-08-17T00:00:00Z"), Instant.now())));
    }

    private void happyAddress() {
        lenient().when(users.getOwnedAddress(anyString(), any())).thenReturn(success(new UserServiceClient.InternalUserAddressResponse(
                addressId, "HOME", null, "Nhà", "Nguyen Khanh", "84912345678", "1 Nguyen Trai", null,
                "District 1", "Ho Chi Minh City", BigDecimal.valueOf(10.7), BigDecimal.valueOf(106.7), null, null, null, "Call first", 2L)));
    }

    private void happyRestaurant(boolean acceptingOrders) {
        lenient().when(restaurants.getCartAvailability(anyString(), any(), any())).thenReturn(success(
                new RestaurantServiceClient.RestaurantBranchCartAvailabilityResponse(
                        restaurantId, "Restaurant", true, branchId, "Branch", true, acceptingOrders)));
    }

    private void happyCatalog(BigDecimal finalUnitPrice) {
        CatalogServiceClient.SelectedOptionResponse option =
                new CatalogServiceClient.SelectedOptionResponse(UUID.randomUUID(), UUID.randomUUID(), "Size", "Large", BigDecimal.TEN);
        lenient().when(catalog.validateCheckoutItems(anyString(), any())).thenReturn(success(
                new CatalogServiceClient.CheckoutItemsValidationResponse(List.of(
                        new CatalogServiceClient.ValidatedCheckoutItemResponse(
                                cartItemId, catalogItemId, UUID.randomUUID(), "Burger", null,
                                finalUnitPrice.subtract(BigDecimal.TEN), null, "VND", List.of(option), BigDecimal.TEN,
                                finalUnitPrice)))));
    }

    private void happyDeliveryQuote() {
        UUID quoteId = UUID.nameUUIDFromBytes("quote".getBytes());
        lenient().when(delivery.createQuote(anyString(), any())).thenReturn(success(new DeliveryServiceClient.DeliveryQuoteResponse(
                quoteId, true, "VND", BigDecimal.valueOf(15000), 4000, 12, "dev-v1",
                Instant.parse("2026-08-17T00:00:00Z"), Instant.parse("2026-08-17T00:05:00Z"))));
    }

    private static <T> RemoteApiResponse<T> success(T data) {
        return new RemoteApiResponse<>(true, "SUCCESS", "", data, Instant.now());
    }

    private void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, ErrorCode expected) {
        assertThatThrownBy(action).isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode()).isEqualTo(expected);
    }
}
