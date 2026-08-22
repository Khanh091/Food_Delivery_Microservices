package com.khanh.fooddelivery.order_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.order_service.client.CartServiceClient;
import com.khanh.fooddelivery.order_service.client.PaymentServiceClient;
import com.khanh.fooddelivery.order_service.client.dto.response.PaymentResponse;
import com.khanh.fooddelivery.order_service.common.response.ApiResponse;
import com.khanh.fooddelivery.order_service.dto.request.CheckoutDeliveryTargetRequest;
import com.khanh.fooddelivery.order_service.dto.request.CreateOrderRequest;
import com.khanh.fooddelivery.order_service.dto.response.CheckoutPreviewResponse;
import com.khanh.fooddelivery.order_service.dto.response.OrderResponse;
import com.khanh.fooddelivery.order_service.entity.Order;
import com.khanh.fooddelivery.order_service.enums.OrderStatus;
import com.khanh.fooddelivery.order_service.enums.PaymentMethod;
import com.khanh.fooddelivery.order_service.enums.PaymentStatus;
import com.khanh.fooddelivery.order_service.exception.AppException;
import com.khanh.fooddelivery.order_service.exception.ErrorCode;
import com.khanh.fooddelivery.order_service.mapper.OrderMapper;
import com.khanh.fooddelivery.order_service.outbox.OrderOutboxService;
import com.khanh.fooddelivery.order_service.placement.OrderPlacementClaim;
import com.khanh.fooddelivery.order_service.placement.OrderPlacementIdempotencyService;
import com.khanh.fooddelivery.order_service.repository.OrderRepository;
import com.khanh.fooddelivery.order_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.order_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.order_service.service.CheckoutPreviewService;
import com.khanh.fooddelivery.order_service.service.RestaurantAuthorizationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import feign.FeignException;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTests {

    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RESTAURANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID BRANCH_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID PLACEMENT_REQUEST_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final UUID RESERVED_ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID CLAIM_TOKEN = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final String IDEMPOTENCY_KEY = "placement-key";

    @Mock private OrderRepository orders;
    @Mock private CheckoutPreviewService checkoutPreviewService;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private CurrentBearerTokenProvider bearerTokenProvider;
    @Mock private CartServiceClient cartClient;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderPlacementRecoveryService orderPlacementRecoveryService;
    @Mock private OrderCreationTransactionService orderCreationTransactionService;
    @Mock private OrderPlacementIdempotencyService orderPlacementIdempotencyService;
    @Mock private RestaurantAuthorizationService restaurantAuthorization;
    @Mock private PaymentServiceClient paymentClient;
    @Mock private OrderOutboxService orderOutbox;
    @Mock private Jwt jwt;

    private OrderServiceImpl service;
    private Order order;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(
                orders,
                checkoutPreviewService,
                currentUserProvider,
                bearerTokenProvider,
                cartClient,
                orderMapper,
                orderPlacementRecoveryService,
                orderCreationTransactionService,
                orderPlacementIdempotencyService,
                restaurantAuthorization,
                paymentClient,
                orderOutbox
        );
        ReflectionTestUtils.setField(service, "internalApiKey", "internal-test-key");

        order = new Order();
        order.setId(ORDER_ID);
        order.setRestaurantId(RESTAURANT_ID);
        order.setBranchId(BRANCH_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setRestaurantName("Restaurant");
        order.setBranchName("Branch");
        order.setStatus(OrderStatus.PENDING_RESTAURANT);
        order.setAddressDisplayLabel("Nhà");
        order.setFormattedAddress("97 Man Thien, Thu Duc, Ho Chi Minh City");
        order.setLatitude(new BigDecimal("10.8000"));
        order.setLongitude(new BigDecimal("106.7500"));
        lenient().when(orders.findWithItemsById(ORDER_ID)).thenReturn(Optional.of(order));
    }

    @Test
    void acceptConfirmsOrderAndPersistsOrderConfirmedOutboxWithoutDeliveryCall() {
        service.accept(null, ORDER_ID);

        verify(orders).saveAndFlush(order);
        verify(orderOutbox).publishOrderConfirmed(order);
        verify(bearerTokenProvider, never()).getBearerToken();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(OrderServiceImpl.class.isAnnotationPresent(Transactional.class)).isFalse();
    }

    @Test
    void createRunsPreflightBeforePlacementAndClearsTheValidatedCartVersionAfterward() {
        CheckoutPreviewResponse preview = preview(7L);
        OrderResponse response = mock(OrderResponse.class);
        CreateOrderRequest request = request(7L, PaymentMethod.COD);
        OrderPlacementClaim claim = activeClaim(7L);
        when(currentUserProvider.getCurrentUserId(jwt)).thenReturn(CUSTOMER_ID);
        when(orderPlacementIdempotencyService.claim(CUSTOMER_ID, IDEMPOTENCY_KEY, request)).thenReturn(claim);
        when(checkoutPreviewService.preview(any(), any())).thenReturn(preview);
        when(orderCreationTransactionService.create(RESERVED_ORDER_ID, CUSTOMER_ID, preview, PaymentMethod.COD))
                .thenReturn(response);
        when(bearerTokenProvider.getBearerToken()).thenReturn("Bearer token");
        when(cartClient.clear("Bearer token", BRANCH_ID, 7L))
                .thenReturn(ApiResponse.success("cleared", null));

        assertThat(service.create(jwt, request, IDEMPOTENCY_KEY)).isSameAs(response);

        var calls = inOrder(orderPlacementIdempotencyService, checkoutPreviewService,
                orderCreationTransactionService, cartClient);
        calls.verify(orderPlacementIdempotencyService).claim(CUSTOMER_ID, IDEMPOTENCY_KEY, request);
        calls.verify(checkoutPreviewService).preview(eq(jwt), any());
        calls.verify(orderCreationTransactionService).create(RESERVED_ORDER_ID, CUSTOMER_ID, preview, PaymentMethod.COD);
        calls.verify(cartClient).clear("Bearer token", BRANCH_ID, 7L);
    }

    @Test
    void createReturnsCommittedOrderResponseWhenPostCommitCartClearFails() {
        CheckoutPreviewResponse preview = preview(7L);
        OrderResponse response = mock(OrderResponse.class);
        CreateOrderRequest request = request(7L, PaymentMethod.ONLINE);
        OrderPlacementClaim claim = activeClaim(7L);
        when(currentUserProvider.getCurrentUserId(jwt)).thenReturn(CUSTOMER_ID);
        when(orderPlacementIdempotencyService.claim(CUSTOMER_ID, IDEMPOTENCY_KEY, request)).thenReturn(claim);
        when(checkoutPreviewService.preview(any(), any())).thenReturn(preview);
        when(orderCreationTransactionService.create(RESERVED_ORDER_ID, CUSTOMER_ID, preview, PaymentMethod.ONLINE))
                .thenReturn(response);
        when(bearerTokenProvider.getBearerToken()).thenReturn("Bearer token");
        FeignException failure = mock(FeignException.class);
        when(cartClient.clear("Bearer token", BRANCH_ID, 7L)).thenThrow(failure);

        assertThat(service.create(jwt, request, IDEMPOTENCY_KEY)).isSameAs(response);
        verify(orderCreationTransactionService).create(RESERVED_ORDER_ID, CUSTOMER_ID, preview, PaymentMethod.ONLINE);
    }

    @Test
    void previewFailureDoesNotCreateOrderOrClearCart() {
        CreateOrderRequest request = request(7L, PaymentMethod.COD);
        when(currentUserProvider.getCurrentUserId(jwt)).thenReturn(CUSTOMER_ID);
        when(orderPlacementIdempotencyService.claim(CUSTOMER_ID, IDEMPOTENCY_KEY, request))
                .thenReturn(activeClaim(7L));
        AppException failure = new AppException(ErrorCode.CART_VERSION_CONFLICT);
        when(checkoutPreviewService.preview(any(), any())).thenThrow(failure);

        assertThatThrownBy(() -> service.create(jwt, request, IDEMPOTENCY_KEY))
                .isSameAs(failure);
        verify(orderCreationTransactionService, never()).create(any(), any(), any(), any());
        verify(cartClient, never()).clear(anyString(), any(), anyLong());
        verify(orderPlacementIdempotencyService).release(PLACEMENT_REQUEST_ID, CLAIM_TOKEN);
    }

    @Test
    void completedReplaySkipsPreviewAndReturnsTheReservedOrder() {
        CreateOrderRequest request = request(7L, PaymentMethod.COD);
        OrderResponse response = mock(OrderResponse.class);
        when(currentUserProvider.getCurrentUserId(jwt)).thenReturn(CUSTOMER_ID);
        when(orderPlacementIdempotencyService.claim(CUSTOMER_ID, IDEMPOTENCY_KEY, request))
                .thenReturn(new OrderPlacementClaim(
                        UUID.randomUUID(), RESERVED_ORDER_ID, BRANCH_ID, 7L, null,
                        OrderPlacementClaim.Status.COMPLETED));
        when(orderPlacementRecoveryService.findResponse(RESERVED_ORDER_ID)).thenReturn(response);
        when(bearerTokenProvider.getBearerToken()).thenReturn("Bearer token");
        when(cartClient.clear("Bearer token", BRANCH_ID, 7L)).thenReturn(ApiResponse.success("cleared", null));

        assertThat(service.create(jwt, request, IDEMPOTENCY_KEY)).isSameAs(response);

        verify(checkoutPreviewService, never()).preview(any(), any());
        verify(orderCreationTransactionService, never()).create(any(), any(), any(), any());
    }

    @Test
    void inProgressReplayRecoversWhenTheReservedOrderAlreadyCommitted() {
        CreateOrderRequest request = request(7L, PaymentMethod.COD);
        OrderResponse response = mock(OrderResponse.class);
        UUID requestId = UUID.randomUUID();
        when(currentUserProvider.getCurrentUserId(jwt)).thenReturn(CUSTOMER_ID);
        when(orderPlacementIdempotencyService.claim(CUSTOMER_ID, IDEMPOTENCY_KEY, request))
                .thenReturn(new OrderPlacementClaim(
                        requestId, RESERVED_ORDER_ID, BRANCH_ID, 7L, null,
                        OrderPlacementClaim.Status.IN_PROGRESS));
        when(orderPlacementRecoveryService.findResponse(RESERVED_ORDER_ID)).thenReturn(response);
        when(bearerTokenProvider.getBearerToken()).thenReturn("Bearer token");
        when(cartClient.clear("Bearer token", BRANCH_ID, 7L)).thenReturn(ApiResponse.success("cleared", null));

        assertThat(service.create(jwt, request, IDEMPOTENCY_KEY)).isSameAs(response);

        verify(orderPlacementIdempotencyService).recoverCompleted(requestId, RESERVED_ORDER_ID);
        verify(checkoutPreviewService, never()).preview(any(), any());
    }

    @Test
    void inProgressReplayWithoutAnOrderReturnsRetryableConflict() {
        CreateOrderRequest request = request(7L, PaymentMethod.COD);
        when(currentUserProvider.getCurrentUserId(jwt)).thenReturn(CUSTOMER_ID);
        when(orderPlacementIdempotencyService.claim(CUSTOMER_ID, IDEMPOTENCY_KEY, request))
                .thenReturn(new OrderPlacementClaim(
                        UUID.randomUUID(), RESERVED_ORDER_ID, BRANCH_ID, 7L, null,
                        OrderPlacementClaim.Status.IN_PROGRESS));

        assertThatThrownBy(() -> service.create(jwt, request, IDEMPOTENCY_KEY))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_IDEMPOTENCY_IN_PROGRESS);
    }

    @Test
    void createAndPlacementTransactionsHaveSeparateBoundaries() throws Exception {
        assertThat(OrderServiceImpl.class.isAnnotationPresent(Transactional.class)).isFalse();
        assertThat(OrderServiceImpl.class
                .getDeclaredMethod("create", Jwt.class, CreateOrderRequest.class, String.class)
                .isAnnotationPresent(Transactional.class)).isFalse();
        assertThat(OrderCreationTransactionService.class
                .getDeclaredMethod("create", UUID.class, UUID.class, CheckoutPreviewResponse.class, PaymentMethod.class)
                .isAnnotationPresent(Transactional.class)).isTrue();
    }

    @Test
    void terminalMatchingFailureCancelsPendingCodPayment() {
        order.setPaymentId(UUID.randomUUID());
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.PENDING);
        when(paymentClient.cancel(eq("internal-test-key"), eq(ORDER_ID)))
                .thenReturn(ApiResponse.success("cancelled", payment(PaymentStatus.CANCELLED)));
        order.setStatus(OrderStatus.CONFIRMED);

        service.matchingFailed(ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(paymentClient).cancel("internal-test-key", ORDER_ID);
    }

    @Test
    void terminalMatchingFailureRefundsPaidOnlinePayment() {
        order.setPaymentId(UUID.randomUUID());
        order.setPaymentMethod(PaymentMethod.ONLINE);
        order.setPaymentStatus(PaymentStatus.PAID);
        when(paymentClient.refund(eq("internal-test-key"), eq(ORDER_ID)))
                .thenReturn(ApiResponse.success("refunded", payment(PaymentStatus.REFUNDED)));
        order.setStatus(OrderStatus.CONFIRMED);

        service.matchingFailed(ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentClient).refund("internal-test-key", ORDER_ID);
    }

    @Test
    void collectedPaymentProjectionCanBeUpdatedIdempotently() {
        order.setPaymentStatus(PaymentStatus.PENDING);

        service.paymentCollected(ORDER_ID);
        service.paymentCollected(ORDER_ID);

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.COLLECTED);
    }

    @Test
    void paymentSucceededMovesPendingPaymentOrderAndDuplicateIsNoOp() {
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.PENDING);

        service.paymentSucceeded(ORDER_ID);
        service.paymentSucceeded(ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_RESTAURANT);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void paymentSucceededAfterOrderAdvancedIsNoOp() {
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.PAID);

        service.paymentSucceeded(ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void paymentFailedUpdatesPendingPaymentProjection() {
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.PROCESSING);

        service.paymentFailed(ORDER_ID);

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void paymentEventCannotApplyImpossibleTransition() {
        order.setStatus(OrderStatus.PENDING_RESTAURANT);
        order.setPaymentStatus(PaymentStatus.PENDING);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.paymentSucceeded(ORDER_ID))
                .isInstanceOf(com.khanh.fooddelivery.order_service.exception.AppException.class);
    }

    private PaymentResponse payment(PaymentStatus status) {
        return new PaymentResponse(
                UUID.randomUUID(), ORDER_ID, order.getPaymentMethod(), status,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    private CreateOrderRequest request(long cartVersion, PaymentMethod paymentMethod) {
        return new CreateOrderRequest(
                BRANCH_ID,
                cartVersion,
                CheckoutDeliveryTargetRequest.savedAddress(UUID.randomUUID()),
                paymentMethod
        );
    }

    private CheckoutPreviewResponse preview(long cartVersion) {
        return new CheckoutPreviewResponse(
                cartVersion,
                new CheckoutPreviewResponse.CheckoutAddressSnapshot(
                        "SAVED_ADDRESS", UUID.randomUUID(), null, "HOME", null, "Home",
                        "Customer", "84912345678", "1 Nguyen Trai", "Ward 1", "District 1",
                        "Ho Chi Minh City", BigDecimal.TEN, BigDecimal.TEN, null, null, null, null, 1L),
                new CheckoutPreviewResponse.CheckoutRestaurantSnapshot(RESTAURANT_ID, "Restaurant"),
                new CheckoutPreviewResponse.CheckoutBranchSnapshot(BRANCH_ID, "Branch"),
                List.of(),
                "VND",
                BigDecimal.valueOf(100),
                BigDecimal.ZERO,
                com.khanh.fooddelivery.order_service.dto.response.DeliveryQuoteStatus.AVAILABLE,
                UUID.randomUUID(),
                Instant.now().plusSeconds(300),
                "dev-v1",
                BigDecimal.valueOf(15000),
                BigDecimal.valueOf(15100),
                List.of(),
                "fingerprint",
                Instant.now(),
                true
        );
    }

    private OrderPlacementClaim activeClaim(long cartVersion) {
        return new OrderPlacementClaim(
                PLACEMENT_REQUEST_ID, RESERVED_ORDER_ID, BRANCH_ID, cartVersion, CLAIM_TOKEN,
                OrderPlacementClaim.Status.ACTIVE);
    }
}
