package com.khanh.fooddelivery.order_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.order_service.client.CartServiceClient;
import com.khanh.fooddelivery.order_service.client.DeliveryServiceClient;
import com.khanh.fooddelivery.order_service.client.PaymentServiceClient;
import com.khanh.fooddelivery.order_service.client.dto.request.DeliveryMatchingRequest;
import com.khanh.fooddelivery.order_service.common.response.ApiResponse;
import com.khanh.fooddelivery.order_service.entity.Order;
import com.khanh.fooddelivery.order_service.enums.OrderStatus;
import com.khanh.fooddelivery.order_service.exception.AppException;
import com.khanh.fooddelivery.order_service.exception.ErrorCode;
import com.khanh.fooddelivery.order_service.mapper.OrderMapper;
import com.khanh.fooddelivery.order_service.repository.OrderRepository;
import com.khanh.fooddelivery.order_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.order_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.order_service.service.CheckoutPreviewService;
import com.khanh.fooddelivery.order_service.service.OrderSnapshotFactory;
import com.khanh.fooddelivery.order_service.service.RestaurantAuthorizationService;
import feign.FeignException;
import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestHeader;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTests {

    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RESTAURANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID BRANCH_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Mock private OrderRepository orders;
    @Mock private CheckoutPreviewService checkoutPreviewService;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private CurrentBearerTokenProvider bearerTokenProvider;
    @Mock private CartServiceClient cartClient;
    @Mock private DeliveryServiceClient deliveryClient;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderSnapshotFactory orderSnapshotFactory;
    @Mock private RestaurantAuthorizationService restaurantAuthorization;
    @Mock private PaymentServiceClient paymentClient;

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
                deliveryClient,
                orderMapper,
                orderSnapshotFactory,
                restaurantAuthorization,
                paymentClient
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
        order.setLatitude(new java.math.BigDecimal("10.8000"));
        order.setLongitude(new java.math.BigDecimal("106.7500"));

        lenient().when(orders.findWithItemsById(ORDER_ID)).thenReturn(Optional.of(order));
        lenient().when(deliveryClient.startMatching(eq("internal-test-key"), any(DeliveryMatchingRequest.class)))
                .thenReturn(ApiResponse.success("Matching started", new Object()));
        lenient().when(orderMapper.toResponse(order)).thenReturn(null);
    }

    @Test
    void acceptUsesInternalApiKeyAndNeverForwardsRestaurantBearerToken() {
        service.accept(null, ORDER_ID);

        verify(deliveryClient).startMatching(eq("internal-test-key"), any(DeliveryMatchingRequest.class));
        verify(bearerTokenProvider, never()).getBearerToken();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void matchingFeignContractUsesInternalHeaderWhileUserScopedCallsKeepAuthorization() throws Exception {
        RequestHeader matchingHeader = DeliveryServiceClient.class
                .getMethod("startMatching", String.class, DeliveryMatchingRequest.class)
                .getParameters()[0]
                .getAnnotation(RequestHeader.class);
        RequestHeader quoteHeader = DeliveryServiceClient.class
                .getMethod("createQuote", String.class, com.khanh.fooddelivery.order_service.client.dto.request.DeliveryQuoteRequest.class)
                .getParameters()[0]
                .getAnnotation(RequestHeader.class);

        assertThat(matchingHeader.value()).isEqualTo("X-Internal-Api-Key");
        assertThat(quoteHeader.value()).isEqualTo(org.springframework.http.HttpHeaders.AUTHORIZATION);
    }

    @Test
    void successfulDeliveryMatchingCompletesAcceptFlow() {
        service.accept(null, ORDER_ID);

        verify(orders).saveAndFlush(order);
        verify(deliveryClient).startMatching(eq("internal-test-key"), any(DeliveryMatchingRequest.class));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void forbiddenDownstreamResponseMapsToAccessDeniedAndLeavesTransactionToRollback() {
        when(deliveryClient.startMatching(eq("internal-test-key"), any(DeliveryMatchingRequest.class)))
                .thenThrow(feignFailure(403, "{\"message\":\"Bearer secret\"}"));

        assertThatThrownBy(() -> service.accept(null, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(OrderServiceImpl.class.isAnnotationPresent(Transactional.class)).isTrue();
        verify(orders).saveAndFlush(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void unavailableDownstreamResponseMapsToServiceUnavailable() {
        when(deliveryClient.startMatching(eq("internal-test-key"), any(DeliveryMatchingRequest.class)))
                .thenThrow(feignFailure(503, "delivery unavailable"));

        assertThatThrownBy(() -> service.accept(null, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.DELIVERY_SERVICE_UNAVAILABLE);
    }

    @Test
    void unsuccessfulHttp200ResponseDoesNotCommitAcceptedOrder() {
        when(deliveryClient.startMatching(eq("internal-test-key"), any(DeliveryMatchingRequest.class)))
                .thenReturn(new ApiResponse<>(false, "DELIVERY_003", "Access denied", null, null));

        assertThatThrownBy(() -> service.accept(null, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.DELIVERY_SERVICE_UNAVAILABLE);
    }

    private FeignException feignFailure(int status, String body) {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "http://delivery-service/internal/v1/deliveries/matching",
                Map.of(),
                new byte[0],
                StandardCharsets.UTF_8
        );
        Response response = Response.builder()
                .status(status)
                .reason("downstream")
                .request(request)
                .body(body, StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus("startMatching", response);
    }
}
