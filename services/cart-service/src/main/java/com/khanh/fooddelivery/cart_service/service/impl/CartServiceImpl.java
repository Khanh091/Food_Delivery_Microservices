package com.khanh.fooddelivery.cart_service.service.impl;

import com.khanh.fooddelivery.cart_service.client.CatalogServiceClient;
import com.khanh.fooddelivery.cart_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.cart_service.config.CartProperties;
import com.khanh.fooddelivery.cart_service.dto.request.AddCartItemRequest;
import com.khanh.fooddelivery.cart_service.dto.request.UpdateCartItemConfigurationRequest;
import com.khanh.fooddelivery.cart_service.dto.request.UpdateCartItemQuantityRequest;
import com.khanh.fooddelivery.cart_service.dto.response.CartItemResponse;
import com.khanh.fooddelivery.cart_service.dto.response.CartResponse;
import com.khanh.fooddelivery.cart_service.dto.response.CartSummaryResponse;
import com.khanh.fooddelivery.cart_service.dto.response.SelectedOptionResponse;
import com.khanh.fooddelivery.cart_service.dto.response.internal.InternalCartItemSnapshotResponse;
import com.khanh.fooddelivery.cart_service.dto.response.internal.InternalCartSnapshotResponse;
import com.khanh.fooddelivery.cart_service.dto.response.internal.InternalSelectedOptionSnapshotResponse;
import com.khanh.fooddelivery.cart_service.exception.AppException;
import com.khanh.fooddelivery.cart_service.exception.ErrorCode;
import com.khanh.fooddelivery.cart_service.model.Cart;
import com.khanh.fooddelivery.cart_service.model.CartItem;
import com.khanh.fooddelivery.cart_service.model.SelectedOption;
import com.khanh.fooddelivery.cart_service.repository.CartRepository;
import com.khanh.fooddelivery.cart_service.repository.CartSnapshot;
import com.khanh.fooddelivery.cart_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.cart_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.cart_service.service.CartFingerprint;
import com.khanh.fooddelivery.cart_service.service.CartService;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private static final int MAX_QUANTITY = 99;
    private static final int SCHEMA_VERSION = 2;

    private final CartRepository carts;
    private final CatalogServiceClient catalogServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;
    private final CurrentUserProvider currentUserProvider;
    private final CurrentBearerTokenProvider bearerTokenProvider;
    private final CartProperties properties;

    @Override
    public List<CartSummaryResponse> list(Jwt jwt) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        return carts.findAll(ownerUserId).stream()
                .filter(snapshot -> !snapshot.cart().items().isEmpty())
                .sorted(Comparator.comparing((CartSnapshot snapshot) -> snapshot.cart().updatedAt()).reversed())
                .map(this::toSummary)
                .toList();
    }

    @Override
    public CartResponse get(Jwt jwt, UUID branchId) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        Optional<CartSnapshot> current = carts.find(ownerUserId, branchId);
        if (current.isPresent()) return toResponse(current.get());
        return emptyResponse(resolveOrderingContext(branchId));
    }

    @Override
    public InternalCartSnapshotResponse getInternalSnapshot(Jwt jwt, UUID branchId) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        return carts.find(ownerUserId, branchId)
                .map(CartSnapshot::cart)
                .map(this::toInternalSnapshot)
                .orElseGet(() -> new InternalCartSnapshotResponse(ownerUserId, null, branchId, null, List.of(), 0, null, null));
    }

    @Override
    public CartResponse add(Jwt jwt, UUID branchId, AddCartItemRequest request) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        RestaurantServiceClient.RestaurantBranchOrderingContextResponse context = requireOrderableContext(branchId);
        ValidatedItem validated = validate(context, branchId, request.catalogItemId(), request.selectedOptionValueIds(), request.note());
        for (int attempt = 0; attempt < properties.casMaxRetries(); attempt++) {
            Cart existing = carts.find(ownerUserId, branchId).map(CartSnapshot::cart).orElse(null);
            Cart candidate = addToCart(ownerUserId, branchId, context, existing, request.quantity(), request.catalogItemId(), validated);
            long expectedVersion = existing == null ? 0 : existing.version();
            if (carts.compareAndSet(ownerUserId, branchId, expectedVersion, candidate)) {
                return toResponse(candidate, Instant.now().plus(properties.ttl()));
            }
        }
        throw new AppException(ErrorCode.CART_VERSION_CONFLICT);
    }

    @Override
    public CartResponse updateQuantity(Jwt jwt, UUID branchId, UUID cartItemId, UpdateCartItemQuantityRequest request) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        RestaurantServiceClient.RestaurantBranchOrderingContextResponse context = requireOrderableContext(branchId);
        for (int attempt = 0; attempt < properties.casMaxRetries(); attempt++) {
            Cart current = requireCart(ownerUserId, branchId);
            CartItem item = requireItem(current, cartItemId);
            ValidatedItem validated = validate(context, branchId, item.catalogItemId(), optionIds(item.selectedOptions()), item.note());
            Cart candidate = replaceItem(current, item.id(), newItem(item.id(), request.quantity(), item.catalogItemId(), validated));
            if (carts.compareAndSet(ownerUserId, branchId, current.version(), candidate)) {
                return toResponse(candidate, Instant.now().plus(properties.ttl()));
            }
        }
        throw new AppException(ErrorCode.CART_VERSION_CONFLICT);
    }

    @Override
    public CartResponse updateConfiguration(
            Jwt jwt, UUID branchId, UUID cartItemId, UpdateCartItemConfigurationRequest request) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        RestaurantServiceClient.RestaurantBranchOrderingContextResponse context = requireOrderableContext(branchId);
        for (int attempt = 0; attempt < properties.casMaxRetries(); attempt++) {
            Cart current = requireCart(ownerUserId, branchId);
            CartItem target = requireItem(current, cartItemId);
            ValidatedItem validated = validate(
                    context, branchId, target.catalogItemId(), request.selectedOptionValueIds(), request.note());
            CartItem edited = newItem(target.id(), request.quantity(), target.catalogItemId(), validated);
            List<CartItem> items = new ArrayList<>(current.items());
            int collision = indexOfFingerprint(items, edited.configurationFingerprint(), target.id());
            if (collision >= 0) {
                CartItem existing = items.get(collision);
                int mergedQuantity = existing.quantity() + request.quantity();
                requireQuantity(mergedQuantity);
                items.set(collision, newItem(existing.id(), mergedQuantity, target.catalogItemId(), validated));
                items.removeIf(item -> item.id().equals(target.id()));
            } else {
                for (int index = 0; index < items.size(); index++) {
                    if (items.get(index).id().equals(target.id())) {
                        items.set(index, edited);
                        break;
                    }
                }
            }
            Cart candidate = copyCart(current, List.copyOf(items), current.version() + 1, Instant.now());
            if (carts.compareAndSet(ownerUserId, branchId, current.version(), candidate)) {
                return toResponse(candidate, Instant.now().plus(properties.ttl()));
            }
        }
        throw new AppException(ErrorCode.CART_VERSION_CONFLICT);
    }

    @Override
    public CartResponse remove(Jwt jwt, UUID branchId, UUID cartItemId) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        for (int attempt = 0; attempt < properties.casMaxRetries(); attempt++) {
            Cart current = requireCart(ownerUserId, branchId);
            List<CartItem> remaining = current.items().stream().filter(item -> !item.id().equals(cartItemId)).toList();
            if (remaining.size() == current.items().size()) throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
            if (remaining.isEmpty()) {
                if (carts.compareAndDelete(ownerUserId, branchId, current.version())) return emptyResponse(current);
            } else {
                Cart candidate = copyCart(current, remaining, current.version() + 1, Instant.now());
                if (carts.compareAndSet(ownerUserId, branchId, current.version(), candidate)) {
                    return toResponse(candidate, Instant.now().plus(properties.ttl()));
                }
            }
        }
        throw new AppException(ErrorCode.CART_VERSION_CONFLICT);
    }

    @Override
    public CartResponse clear(Jwt jwt, UUID branchId, long expectedCartVersion) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        for (int attempt = 0; attempt < properties.casMaxRetries(); attempt++) {
            Optional<CartSnapshot> current = carts.find(ownerUserId, branchId);
            if (current.isEmpty()) return emptyResponse(branchId);
            if (current.get().cart().version() != expectedCartVersion) {
                throw new AppException(ErrorCode.CART_VERSION_CONFLICT);
            }
            if (carts.compareAndDelete(ownerUserId, branchId, expectedCartVersion)) {
                return emptyResponse(current.get().cart());
            }
        }
        throw new AppException(ErrorCode.CART_VERSION_CONFLICT);
    }

    private Cart requireCart(UUID ownerUserId, UUID branchId) {
        return carts.find(ownerUserId, branchId).map(CartSnapshot::cart)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));
    }

    private CartItem requireItem(Cart cart, UUID cartItemId) {
        return cart.items().stream().filter(item -> item.id().equals(cartItemId)).findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));
    }

    private RestaurantServiceClient.RestaurantBranchOrderingContextResponse resolveOrderingContext(UUID branchId) {
        try {
            RestaurantServiceClient.ApiResponse<RestaurantServiceClient.RestaurantBranchOrderingContextResponse> response =
                    restaurantServiceClient.getOrderingContext(bearerTokenProvider.getBearerToken(), branchId);
            if (response == null || !response.success() || response.data() == null) {
                throw new AppException(ErrorCode.RESTAURANT_SERVICE_UNAVAILABLE);
            }
            RestaurantServiceClient.RestaurantBranchOrderingContextResponse context = response.data();
            return context;
        } catch (FeignException.NotFound exception) {
            throw new AppException(ErrorCode.BRANCH_NOT_ACCEPTING_ORDERS);
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.RESTAURANT_SERVICE_UNAVAILABLE);
        }
    }

    private RestaurantServiceClient.RestaurantBranchOrderingContextResponse requireOrderableContext(UUID branchId) {
        RestaurantServiceClient.RestaurantBranchOrderingContextResponse context = resolveOrderingContext(branchId);
        if (!context.restaurantActive() || !context.branchActive() || !context.acceptingOrders()) {
            throw new AppException(ErrorCode.BRANCH_NOT_ACCEPTING_ORDERS);
        }
        return context;
    }

    private ValidatedItem validate(
            RestaurantServiceClient.RestaurantBranchOrderingContextResponse context,
            UUID branchId,
            UUID catalogItemId,
            List<UUID> selectedOptionValueIds,
            String note) {
        String normalizedNote = normalizeNote(note);
        try {
            CatalogServiceClient.ApiResponse<CatalogServiceClient.CartItemValidationResponse> response =
                    catalogServiceClient.validateCartItem(
                            bearerTokenProvider.getBearerToken(),
                            new CatalogServiceClient.CartItemValidationRequest(
                                    context.restaurantId(), branchId, catalogItemId, selectedOptionValueIds));
            if (response == null || !response.success() || response.data() == null) {
                throw new AppException(ErrorCode.CATALOG_SERVICE_UNAVAILABLE);
            }
            return new ValidatedItem(response.data(), normalizedNote, selectedOptionValueIds);
        } catch (FeignException exception) {
            throw catalogFailure(exception);
        }
    }

    private AppException catalogFailure(FeignException exception) {
        String body = exception.contentUTF8();
        if (exception.status() == 404) {
            return new AppException(body.contains("CATALOG_016")
                    ? ErrorCode.BRANCH_ITEM_NOT_FOUND : ErrorCode.CATALOG_ITEM_NOT_FOUND);
        }
        if (exception.status() == 400) return new AppException(ErrorCode.INVALID_OPTION_SELECTION);
        if (exception.status() == 409) return new AppException(ErrorCode.ITEM_UNAVAILABLE);
        return new AppException(ErrorCode.CATALOG_SERVICE_UNAVAILABLE);
    }

    private Cart addToCart(
            UUID ownerUserId,
            UUID branchId,
            RestaurantServiceClient.RestaurantBranchOrderingContextResponse context,
            Cart existing,
            int quantity,
            UUID catalogItemId,
            ValidatedItem validated) {
        requireQuantity(quantity);
        CartItem newItem = newItem(UUID.randomUUID(), quantity, catalogItemId, validated);
        Instant now = Instant.now();
        if (existing == null) {
            return new Cart(
                    SCHEMA_VERSION, ownerUserId, context.restaurantId(), branchId,
                    context.restaurantName(), context.branchName(), validated.catalog().currency(),
                    List.of(newItem), 1, now, now);
        }
        List<CartItem> items = new ArrayList<>(existing.items());
        int match = indexOfFingerprint(items, newItem.configurationFingerprint(), null);
        if (match >= 0) {
            CartItem prior = items.get(match);
            int mergedQuantity = prior.quantity() + quantity;
            requireQuantity(mergedQuantity);
            items.set(match, newItem(prior.id(), mergedQuantity, catalogItemId, validated));
        } else {
            items.add(newItem);
        }
        return copyCart(existing, List.copyOf(items), existing.version() + 1, now);
    }

    private Cart replaceItem(Cart cart, UUID targetId, CartItem replacement) {
        List<CartItem> items = cart.items().stream()
                .map(item -> item.id().equals(targetId) ? replacement : item).toList();
        return copyCart(cart, items, cart.version() + 1, Instant.now());
    }

    private CartItem newItem(UUID id, int quantity, UUID catalogItemId, ValidatedItem validated) {
        return new CartItem(
                id,
                CartFingerprint.of(catalogItemId, validated.optionIds(), validated.note()),
                validated.catalog().catalogItemId(),
                validated.catalog().branchItemId(),
                quantity,
                validated.note(),
                selectedOptions(validated.catalog()),
                validated.catalog().itemName(),
                validated.catalog().primaryImageUrl(),
                validated.catalog().sellingPrice(),
                validated.catalog().optionUnitPrice(),
                validated.catalog().finalUnitPrice(),
                validated.catalog().originalPrice());
    }

    private List<SelectedOption> selectedOptions(CatalogServiceClient.CartItemValidationResponse catalog) {
        return catalog.selectedOptions().stream()
                .map(option -> new SelectedOption(
                        option.optionGroupId(), option.optionValueId(), option.groupName(), option.valueName(), option.additionalPrice()))
                .toList();
    }

    private Cart copyCart(Cart source, List<CartItem> items, long version, Instant updatedAt) {
        return new Cart(
                source.schemaVersion(), source.ownerUserId(), source.restaurantId(), source.branchId(),
                source.restaurantNameSnapshot(), source.branchNameSnapshot(), source.currency(), items,
                version, source.createdAt(), updatedAt);
    }

    private int indexOfFingerprint(List<CartItem> items, String fingerprint, UUID excludedId) {
        for (int index = 0; index < items.size(); index++) {
            CartItem item = items.get(index);
            if ((excludedId == null || !item.id().equals(excludedId)) && item.configurationFingerprint().equals(fingerprint)) {
                return index;
            }
        }
        return -1;
    }

    private List<UUID> optionIds(List<SelectedOption> options) {
        return options.stream().map(SelectedOption::optionValueId).toList();
    }

    private void requireQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY) throw new AppException(ErrorCode.QUANTITY_OUT_OF_RANGE);
    }

    private String normalizeNote(String note) {
        if (note == null) return null;
        String normalized = note.trim();
        if (normalized.length() > 500) throw new AppException(ErrorCode.INVALID_REQUEST, "Note must not exceed 500 characters");
        return normalized.isEmpty() ? null : normalized;
    }

    private CartSummaryResponse toSummary(CartSnapshot snapshot) {
        Cart cart = snapshot.cart();
        return new CartSummaryResponse(
                cart.restaurantId(), cart.restaurantNameSnapshot(), cart.branchId(), cart.branchNameSnapshot(),
                cart.items().stream().mapToInt(CartItem::quantity).sum(), subtotal(cart.items()), cart.currency(),
                cart.version(), cart.updatedAt(), snapshot.expiresAt());
    }

    private CartResponse toResponse(CartSnapshot snapshot) {
        return toResponse(snapshot.cart(), snapshot.expiresAt());
    }

    private CartResponse toResponse(Cart cart, Instant expiresAt) {
        List<CartItemResponse> items = cart.items().stream().map(this::toResponse).toList();
        return new CartResponse(
                cart.restaurantId(), cart.restaurantNameSnapshot(), cart.branchId(), cart.branchNameSnapshot(),
                cart.currency(), items, items.stream().map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add),
                items.stream().mapToInt(CartItemResponse::quantity).sum(), cart.version(), cart.createdAt(), cart.updatedAt(), expiresAt);
    }

    private CartItemResponse toResponse(CartItem item) {
        return new CartItemResponse(
                item.id(), item.catalogItemId(), item.branchItemId(), item.itemNameSnapshot(), item.imageUrlSnapshot(),
                item.quantity(), item.note(), item.selectedOptions().stream().map(option -> new SelectedOptionResponse(
                        option.optionGroupId(), option.optionValueId(), option.groupNameSnapshot(), option.valueNameSnapshot(), option.additionalPriceSnapshot())).toList(),
                item.baseUnitPriceSnapshot(), item.optionUnitPriceSnapshot(), item.unitPriceSnapshot(), item.originalPriceSnapshot(), item.lineTotal());
    }

    private InternalCartSnapshotResponse toInternalSnapshot(Cart cart) {
        return new InternalCartSnapshotResponse(
                cart.ownerUserId(), cart.restaurantId(), cart.branchId(), cart.currency(), cart.items().stream().map(item ->
                        new InternalCartItemSnapshotResponse(item.id(), item.catalogItemId(), item.branchItemId(), item.quantity(), item.note(),
                                item.selectedOptions().stream().map(option -> new InternalSelectedOptionSnapshotResponse(
                                        option.optionGroupId(), option.optionValueId(), option.groupNameSnapshot(), option.valueNameSnapshot(), option.additionalPriceSnapshot())).toList(),
                                item.itemNameSnapshot(), item.imageUrlSnapshot(), item.baseUnitPriceSnapshot(), item.optionUnitPriceSnapshot(),
                                item.unitPriceSnapshot(), item.originalPriceSnapshot())).toList(),
                cart.version(), cart.createdAt(), cart.updatedAt());
    }

    private BigDecimal subtotal(List<CartItem> items) {
        return items.stream().map(CartItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CartResponse emptyResponse(RestaurantServiceClient.RestaurantBranchOrderingContextResponse context) {
        return new CartResponse(context.restaurantId(), context.restaurantName(), context.branchId(), context.branchName(),
                null, List.of(), BigDecimal.ZERO, 0, 0, null, null, null);
    }

    private CartResponse emptyResponse(Cart cart) {
        return new CartResponse(cart.restaurantId(), cart.restaurantNameSnapshot(), cart.branchId(), cart.branchNameSnapshot(),
                cart.currency(), List.of(), BigDecimal.ZERO, 0, 0, null, null, null);
    }

    private CartResponse emptyResponse(UUID branchId) {
        return new CartResponse(null, null, branchId, null, null, List.of(), BigDecimal.ZERO, 0, 0, null, null, null);
    }

    private record ValidatedItem(
            CatalogServiceClient.CartItemValidationResponse catalog,
            String note,
            List<UUID> optionIds) {}
}
