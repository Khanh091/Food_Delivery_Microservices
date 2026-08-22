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
import com.khanh.fooddelivery.order_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.order_service.client.UserServiceClient;
import com.khanh.fooddelivery.order_service.client.dto.request.CheckoutItemsValidationRequest;
import com.khanh.fooddelivery.order_service.client.dto.response.*;
import com.khanh.fooddelivery.order_service.common.response.ApiResponse;
import com.khanh.fooddelivery.order_service.dto.request.CheckoutPreviewRequest;
import com.khanh.fooddelivery.order_service.dto.request.CheckoutDeliveryTargetRequest;
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
        assertThat(preview.address().formattedAddress())
                .isEqualTo("1 Nguyen Trai, District 1, Ho Chi Minh City");
        assertThat(preview.priceChanges()).singleElement()
                .satisfies(change -> assertThat(change.currentUnitPrice()).isEqualByComparingTo("55"));
        verify(catalog, times(1)).validateCheckoutItems(anyString(), any());
    }

    @Test
    void emptyCartIsRejected() {
        when(carts.getCurrentSnapshot(anyString(), any())).thenReturn(success(new InternalCartSnapshotResponse(
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
        when(users.getOwnedAddress(anyString(), any())).thenReturn(success(new InternalUserAddressResponse(
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
    void temporaryTargetUsesItsServerOwnedReferenceWithoutLoadingAnySavedAddress() {
        UUID temporaryLocationId = UUID.randomUUID();
        when(users.getCurrentUser("Bearer token")).thenReturn(success(
                new CurrentUserResponse(ownerId, "+84912345678", "Nguyen Van Khanh", "84912345678", "customer@example.com")));
        when(delivery.getCurrentCheckoutLocation("Bearer token", branchId)).thenReturn(success(
                new CheckoutTemporaryLocationResponse(
                        temporaryLocationId, branchId, "Temporary location", "Temporary location", null, null, null,
                        BigDecimal.valueOf(10.8), BigDecimal.valueOf(106.8), Instant.now().plusSeconds(2700))));

        var preview = service.preview(jwt, new CheckoutPreviewRequest(branchId, 3L,
                new CheckoutDeliveryTargetRequest("TEMPORARY_LOCATION", null, temporaryLocationId)));

        assertThat(preview.address().targetType()).isEqualTo("TEMPORARY_LOCATION");
        assertThat(preview.address().addressId()).isNull();
        assertThat(preview.address().temporaryLocationId()).isEqualTo(temporaryLocationId);
        assertThat(preview.address().recipientName()).isEqualTo("Nguyen Van Khanh");
        assertThat(preview.address().formattedAddress()).isEqualTo("Temporary location");
        assertThat(preview.address().recipientPhone()).isEqualTo("+84912345678");
        assertThat(preview.deliveryQuoteStatus())
                .isEqualTo(com.khanh.fooddelivery.order_service.dto.response.DeliveryQuoteStatus.AVAILABLE);
        verify(users, never()).getOwnedAddress(anyString(), any());
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
        InternalSelectedOptionSnapshotResponse option =
                new InternalSelectedOptionSnapshotResponse(
                        UUID.randomUUID(), UUID.randomUUID(), "Size", "Large", BigDecimal.TEN);
        InternalCartItemSnapshotResponse item =
                new InternalCartItemSnapshotResponse(
                        cartItemId, catalogItemId, UUID.randomUUID(), 2, "no onion", List.of(option), "Burger", null,
                        BigDecimal.valueOf(40), BigDecimal.TEN, BigDecimal.valueOf(50), null);
        when(carts.getCurrentSnapshot(anyString(), any())).thenReturn(success(new InternalCartSnapshotResponse(
                ownerId, restaurantId, branchId, "VND", List.of(item), version, Instant.parse("2026-08-17T00:00:00Z"), Instant.now())));
    }

    private void happyAddress() {
        lenient().when(users.getOwnedAddress(anyString(), any())).thenReturn(success(new InternalUserAddressResponse(
                addressId, "HOME", null, "Nhà", "Nguyen Khanh", "84912345678", "1 Nguyen Trai", null,
                "District 1", "Ho Chi Minh City", BigDecimal.valueOf(10.7), BigDecimal.valueOf(106.7), null, null, null, "Call first", 2L)));
    }

    private void happyRestaurant(boolean acceptingOrders) {
        lenient().when(restaurants.getCartAvailability(anyString(), any(), any())).thenReturn(success(
                new RestaurantBranchCartAvailabilityResponse(
                        restaurantId, "Restaurant", true, branchId, "Branch", true, acceptingOrders)));
    }

    private void happyCatalog(BigDecimal finalUnitPrice) {
        SelectedOptionResponse option =
                new SelectedOptionResponse(UUID.randomUUID(), UUID.randomUUID(), "Size", "Large", BigDecimal.TEN);
        lenient().when(catalog.validateCheckoutItems(anyString(), any())).thenReturn(success(
                new CheckoutItemsValidationResponse(List.of(
                        new ValidatedCheckoutItemResponse(
                                cartItemId, catalogItemId, UUID.randomUUID(), "Burger", null,
                                finalUnitPrice.subtract(BigDecimal.TEN), null, "VND", List.of(option), BigDecimal.TEN,
                                finalUnitPrice)))));
    }

    private void happyDeliveryQuote() {
        UUID quoteId = UUID.nameUUIDFromBytes("quote".getBytes());
        lenient().when(delivery.createQuote(anyString(), any())).thenReturn(success(new DeliveryQuoteResponse(
                quoteId, true, "VND", BigDecimal.valueOf(15000), 4000, 12, "dev-v1",
                Instant.parse("2026-08-17T00:00:00Z"), Instant.parse("2026-08-17T00:05:00Z"))));
    }

    private static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "SUCCESS", "", data, Instant.now());
    }

    private void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, ErrorCode expected) {
        assertThatThrownBy(action).isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode()).isEqualTo(expected);
    }
}
