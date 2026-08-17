package com.khanh.fooddelivery.cart_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.cart_service.client.CatalogServiceClient;
import com.khanh.fooddelivery.cart_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.cart_service.config.CartProperties;
import com.khanh.fooddelivery.cart_service.dto.request.AddCartItemRequest;
import com.khanh.fooddelivery.cart_service.dto.request.UpdateCartItemConfigurationRequest;
import com.khanh.fooddelivery.cart_service.dto.request.UpdateCartItemQuantityRequest;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServiceImplTests {
    private final UUID ownerId = UUID.randomUUID();
    private final UUID restaurantA = UUID.randomUUID();
    private final UUID restaurantB = UUID.randomUUID();
    private final UUID branchA = UUID.randomUUID();
    private final UUID branchB = UUID.randomUUID();
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
        service = new CartServiceImpl(carts, catalog, restaurant, currentUser, bearer, new CartProperties(Duration.ofDays(7), 3));
        when(currentUser.getCurrentUserId(jwt)).thenReturn(ownerId);
        when(bearer.getBearerToken()).thenReturn("Bearer token");
        when(restaurant.getOrderingContext(anyString(), any())).thenAnswer(invocation -> {
            UUID branchId = invocation.getArgument(1);
            UUID restaurantId = branchId.equals(branchA) ? restaurantA : restaurantB;
            return success(new RestaurantServiceClient.RestaurantBranchOrderingContextResponse(
                    restaurantId, "Restaurant " + branchId, true, branchId, "Branch " + branchId, true, true,
                    BigDecimal.TEN, BigDecimal.TEN));
        });
        when(catalog.validateCartItem(anyString(), any())).thenAnswer(invocation -> {
            CatalogServiceClient.CartItemValidationRequest request = invocation.getArgument(1);
            List<CatalogServiceClient.SelectedOptionResponse> selected = request.selectedOptionValueIds().stream()
                    .sorted().map(id -> new CatalogServiceClient.SelectedOptionResponse(
                            UUID.randomUUID(), id, "Group", "Option", BigDecimal.TEN)).toList();
            return catalogSuccess(new CatalogServiceClient.CartItemValidationResponse(
                    request.catalogItemId(), UUID.randomUUID(), "Item", null, BigDecimal.valueOf(100), null, "VND",
                    selected, BigDecimal.TEN.multiply(BigDecimal.valueOf(selected.size())),
                    BigDecimal.valueOf(100).add(BigDecimal.TEN.multiply(BigDecimal.valueOf(selected.size())))));
        });
    }

    @Test
    void branchCartsAreIndependentAndUseAuthoritativeRestaurantContext() {
        CartResponse a = service.add(jwt, branchA, request(itemId, 1, List.of(), null));
        CartResponse b = service.add(jwt, branchB, request(itemId, 2, List.of(), null));

        assertThat(a.restaurantId()).isEqualTo(restaurantA);
        assertThat(b.restaurantId()).isEqualTo(restaurantB);
        assertThat(service.get(jwt, branchA).totalQuantity()).isEqualTo(1);
        assertThat(service.get(jwt, branchB).totalQuantity()).isEqualTo(2);
        verify(catalog, times(2)).validateCartItem(anyString(), any(CatalogServiceClient.CartItemValidationRequest.class));
    }

    @Test
    void updateAndClearOnlyAffectTheRequestedBranch() {
        CartResponse a = service.add(jwt, branchA, request(itemId, 1, List.of(), null));
        service.add(jwt, branchB, request(itemId, 2, List.of(), null));

        service.updateQuantity(jwt, branchA, a.items().getFirst().cartItemId(), new UpdateCartItemQuantityRequest(3));
        service.clear(jwt, branchA);

        assertThat(service.get(jwt, branchA).items()).isEmpty();
        assertThat(service.get(jwt, branchB).totalQuantity()).isEqualTo(2);
    }

    @Test
    void configurationEditMergesWithExistingEquivalentLine() {
        CartResponse cart = service.add(jwt, branchA, request(itemId, 1, List.of(optionA), null));
        CartResponse distinct = service.add(jwt, branchA, request(itemId, 2, List.of(optionB), null));
        UUID changingLine = distinct.items().stream()
                .filter(item -> item.selectedOptions().stream().anyMatch(option -> option.optionValueId().equals(optionB)))
                .findFirst().orElseThrow().cartItemId();

        CartResponse merged = service.updateConfiguration(
                jwt, branchA, changingLine, new UpdateCartItemConfigurationRequest(2, List.of(optionA), null));

        assertThat(merged.items()).singleElement().satisfies(item -> assertThat(item.quantity()).isEqualTo(3));
        assertThat(merged.version()).isEqualTo(3);
        assertThat(cart.items()).hasSize(1);
    }

    @Test
    void listReturnsOnlyNonEmptyBranchCartsSortedByUpdateTime() {
        service.add(jwt, branchA, request(itemId, 1, List.of(), null));
        service.add(jwt, branchB, request(itemId, 2, List.of(), null));

        assertThat(service.list(jwt)).extracting(summary -> summary.branchId()).containsExactlyInAnyOrder(branchA, branchB);
    }

    @Test
    void fingerprintIgnoresOptionOrder() {
        assertThat(CartFingerprint.of(itemId, List.of(optionA, optionB), "no onion"))
                .isEqualTo(CartFingerprint.of(itemId, List.of(optionB, optionA), "no onion"));
    }

    @Test
    void catalogFailureDoesNotPersistCandidateCart() {
        org.mockito.Mockito.doReturn(new CatalogServiceClient.ApiResponse<>(false, "ERROR", "", null, Instant.now()))
                .when(catalog).validateCartItem(anyString(), any());

        assertThatThrownBy(() -> service.add(jwt, branchA, request(itemId, 1, List.of(), null)))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CATALOG_SERVICE_UNAVAILABLE);
        assertThat(carts.find(ownerId, branchA)).isEmpty();
    }

    private AddCartItemRequest request(UUID catalogItemId, int quantity, List<UUID> options, String note) {
        return new AddCartItemRequest(catalogItemId, quantity, options, note);
    }

    private static <T> RestaurantServiceClient.ApiResponse<T> success(T data) {
        return new RestaurantServiceClient.ApiResponse<>(true, "SUCCESS", "", data, Instant.now());
    }

    private static <T> CatalogServiceClient.ApiResponse<T> catalogSuccess(T data) {
        return new CatalogServiceClient.ApiResponse<>(true, "SUCCESS", "", data, Instant.now());
    }

    private static final class InMemoryCartRepository implements CartRepository {
        private final Map<UUID, Cart> carts = new HashMap<>();

        @Override
        public Optional<CartSnapshot> find(UUID ownerUserId, UUID branchId) {
            Cart cart = carts.get(branchId);
            return cart == null ? Optional.empty() : Optional.of(new CartSnapshot(cart, Instant.now().plus(Duration.ofDays(7))));
        }

        @Override
        public List<CartSnapshot> findAll(UUID ownerUserId) {
            return carts.values().stream()
                    .map(cart -> new CartSnapshot(cart, Instant.now().plus(Duration.ofDays(7))))
                    .sorted(Comparator.comparing(snapshot -> snapshot.cart().updatedAt()))
                    .toList();
        }

        @Override
        public boolean compareAndSet(UUID ownerUserId, UUID branchId, long expectedVersion, Cart candidate) {
            Cart current = carts.get(branchId);
            if ((current == null && expectedVersion != 0) || (current != null && current.version() != expectedVersion)) return false;
            carts.put(branchId, candidate);
            return true;
        }

        @Override
        public boolean compareAndDelete(UUID ownerUserId, UUID branchId, long expectedVersion) {
            Cart current = carts.get(branchId);
            if (current == null || current.version() != expectedVersion) return false;
            carts.remove(branchId);
            return true;
        }
    }
}
