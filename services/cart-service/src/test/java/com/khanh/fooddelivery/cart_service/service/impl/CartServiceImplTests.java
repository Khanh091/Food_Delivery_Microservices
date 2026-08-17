package com.khanh.fooddelivery.cart_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.cart_service.client.CatalogServiceClient;
import com.khanh.fooddelivery.cart_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.cart_service.client.UserServiceClient;
import com.khanh.fooddelivery.cart_service.config.CartProperties;
import com.khanh.fooddelivery.cart_service.dto.request.AddCartItemRequest;
import com.khanh.fooddelivery.cart_service.dto.request.ReplaceCartItemRequest;
import com.khanh.fooddelivery.cart_service.dto.response.CartResponse;
import com.khanh.fooddelivery.cart_service.exception.AppException;
import com.khanh.fooddelivery.cart_service.exception.ErrorCode;
import com.khanh.fooddelivery.cart_service.model.Cart;
import com.khanh.fooddelivery.cart_service.repository.CartRepository;
import com.khanh.fooddelivery.cart_service.repository.CartSnapshot;
import com.khanh.fooddelivery.cart_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.cart_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.cart_service.service.CartFingerprint;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServiceImplTests {
    private final UUID ownerId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final UUID optionA = UUID.randomUUID();
    private final UUID optionB = UUID.randomUUID();

    @Mock private CatalogServiceClient catalog;
    @Mock private RestaurantServiceClient restaurant;
    @Mock private CurrentUserProvider currentUser;
    @Mock private CurrentBearerTokenProvider bearer;
    @Mock private Jwt jwt;

    private InMemoryCartRepository carts;
    private CartServiceImpl service;

    @BeforeEach
    void setUp() {
        carts = new InMemoryCartRepository();
        service =
                new CartServiceImpl(
                        carts,
                        catalog,
                        restaurant,
                        currentUser,
                        bearer,
                        new CartProperties(Duration.ofDays(7), 3));
        when(currentUser.getCurrentUserId(jwt)).thenReturn(ownerId);
        when(bearer.getBearerToken()).thenReturn("Bearer token");
        when(restaurant.getCartAvailability(anyString(), any(), any()))
                .thenReturn(
                        new RestaurantServiceClient.ApiResponse<>(
                                true,
                                "SUCCESS",
                                "",
                                new RestaurantServiceClient.RestaurantBranchCartAvailabilityResponse(
                                        restaurantId,
                                        "Restaurant",
                                        true,
                                        branchId,
                                        "Branch",
                                        true,
                                        true),
                                Instant.now()));
        when(catalog.validateCartItem(anyString(), any()))
                .thenAnswer(
                        invocation -> {
                            CatalogServiceClient.CartItemValidationRequest request = invocation.getArgument(1);
                            List<CatalogServiceClient.SelectedOptionResponse> selected =
                                    request.selectedOptionValueIds().stream()
                                            .sorted()
                                            .map(
                                                    id ->
                                                            new CatalogServiceClient.SelectedOptionResponse(
                                                                    UUID.randomUUID(),
                                                                    id,
                                                                    "Group",
                                                                    "Option",
                                                                    BigDecimal.TEN))
                                            .toList();
                            return new CatalogServiceClient.ApiResponse<>(
                                    true,
                                    "SUCCESS",
                                    "",
                                    new CatalogServiceClient.CartItemValidationResponse(
                                            itemId,
                                            UUID.randomUUID(),
                                            "Item",
                                            null,
                                            BigDecimal.valueOf(100),
                                            null,
                                            "VND",
                                            selected,
                                            BigDecimal.TEN.multiply(BigDecimal.valueOf(selected.size())),
                                            BigDecimal.valueOf(100)
                                                    .add(BigDecimal.TEN.multiply(BigDecimal.valueOf(selected.size())))),
                                    Instant.now());
                        });
    }

    @Test
    void fingerprintIgnoresOptionOrder() {
        assertThat(CartFingerprint.of(itemId, List.of(optionA, optionB), "no onion"))
                .isEqualTo(CartFingerprint.of(itemId, List.of(optionB, optionA), "no onion"));
    }

    @Test
    void sameConfigurationMergesQuantityAndDifferentNoteCreatesNewLine() {
        service.add(jwt, request(1, List.of(optionA, optionB), " no onion "));
        CartResponse merged = service.add(jwt, request(2, List.of(optionB, optionA), "no onion"));

        assertThat(merged.items()).hasSize(1);
        assertThat(merged.items().getFirst().quantity()).isEqualTo(3);

        CartResponse distinct = service.add(jwt, request(1, List.of(optionA, optionB), "extra spicy"));
        assertThat(distinct.items()).hasSize(2);
    }

    @Test
    void addingDifferentBranchReturnsConflictWithoutReplacingCart() {
        service.add(jwt, request(1, List.of(), null));
        AddCartItemRequest otherBranch =
                new AddCartItemRequest(restaurantId, UUID.randomUUID(), itemId, 1, List.of(), null);

        assertThatThrownBy(() -> service.add(jwt, otherBranch))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CART_DIFFERENT_BRANCH);
        assertThat(carts.cart).isNotNull();
        assertThat(carts.cart.branchId()).isEqualTo(branchId);
    }

    @Test
    void staleReplaceVersionIsRejected() {
        service.add(jwt, request(1, List.of(), null));

        assertThatThrownBy(
                        () ->
                                service.replace(
                                        jwt,
                                        new ReplaceCartItemRequest(2L, request(1, List.of(), null))))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CART_VERSION_CONFLICT);
    }

    @Test
    void catalogFailureDoesNotPersistCandidateCart() {
        org.mockito.Mockito.doReturn(
                        new CatalogServiceClient.ApiResponse<>(
                                false, "ERROR", "", null, Instant.now()))
                .when(catalog)
                .validateCartItem(anyString(), any());

        assertThatThrownBy(() -> service.add(jwt, request(1, List.of(), null)))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CATALOG_SERVICE_UNAVAILABLE);
        assertThat(carts.cart).isNull();
    }

    @Test
    void removingLastItemDeletesCart() {
        CartResponse added = service.add(jwt, request(1, List.of(), null));

        CartResponse empty = service.remove(jwt, added.items().getFirst().cartItemId());

        assertThat(empty.items()).isEmpty();
        assertThat(carts.cart).isNull();
    }

    private AddCartItemRequest request(int quantity, List<UUID> options, String note) {
        return new AddCartItemRequest(restaurantId, branchId, itemId, quantity, options, note);
    }

    private static final class InMemoryCartRepository implements CartRepository {
        private Cart cart;

        @Override
        public Optional<CartSnapshot> find(UUID ownerUserId) {
            return cart == null
                    ? Optional.empty()
                    : Optional.of(new CartSnapshot(cart, Instant.now().plus(Duration.ofDays(7))));
        }

        @Override
        public boolean compareAndSet(UUID ownerUserId, long expectedVersion, Cart candidate) {
            if ((cart == null && expectedVersion != 0)
                    || (cart != null && cart.version() != expectedVersion)) return false;
            cart = candidate;
            return true;
        }

        @Override
        public boolean compareAndDelete(UUID ownerUserId, long expectedVersion) {
            if (cart == null || cart.version() != expectedVersion) return false;
            cart = null;
            return true;
        }
    }
}
