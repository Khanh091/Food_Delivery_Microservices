package com.khanh.fooddelivery.order_service.placement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khanh.fooddelivery.order_service.dto.request.CheckoutDeliveryTargetRequest;
import com.khanh.fooddelivery.order_service.dto.request.CreateOrderRequest;
import com.khanh.fooddelivery.order_service.enums.PaymentMethod;
import com.khanh.fooddelivery.order_service.exception.AppException;
import com.khanh.fooddelivery.order_service.exception.ErrorCode;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        OrderPlacementIdempotencyService.class,
        OrderPlacementFingerprint.class,
        OrderPlacementIdempotencyIntegrationTests.TestClockConfiguration.class
})
@Testcontainers
class OrderPlacementIdempotencyIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private OrderPlacementIdempotencyService service;

    @Autowired
    private OrderPlacementRequestRepository requests;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @AfterEach
    void clean() {
        requests.deleteAll();
    }

    @Test
    void firstClaimReservesStableOrderIdAndReplayIsInProgress() {
        UUID customerId = UUID.randomUUID();
        CreateOrderRequest request = request(UUID.randomUUID(), 4L, PaymentMethod.COD);

        OrderPlacementClaim first = service.claim(customerId, "key-1", request);
        OrderPlacementClaim replay = service.claim(customerId, "key-1", request);

        assertThat(first.status()).isEqualTo(OrderPlacementClaim.Status.ACTIVE);
        assertThat(replay.status()).isEqualTo(OrderPlacementClaim.Status.IN_PROGRESS);
        assertThat(replay.reservedOrderId()).isEqualTo(first.reservedOrderId());
        assertThat(requests.count()).isEqualTo(1);
    }

    @Test
    void completedReplayReturnsTheSameStableOrderId() {
        UUID customerId = UUID.randomUUID();
        CreateOrderRequest request = request(UUID.randomUUID(), 4L, PaymentMethod.COD);

        OrderPlacementClaim first = service.claim(customerId, "key-2", request);
        assertThat(service.markCompleted(first.requestId(), first.claimToken())).isTrue();

        OrderPlacementClaim replay = service.claim(customerId, "key-2", request);

        assertThat(replay.status()).isEqualTo(OrderPlacementClaim.Status.COMPLETED);
        assertThat(replay.reservedOrderId()).isEqualTo(first.reservedOrderId());
    }

    @Test
    void staleProcessingRecordCanResumeWithTheSameStableOrderId() {
        UUID customerId = UUID.randomUUID();
        CreateOrderRequest request = request(UUID.randomUUID(), 4L, PaymentMethod.COD);

        OrderPlacementClaim first = service.claim(customerId, "stale-key", request);
        jdbc.update(
                "UPDATE order_placement_requests SET processing_until = CURRENT_TIMESTAMP - INTERVAL '1 second' WHERE id = ?",
                first.requestId()
        );

        OrderPlacementClaim recovered = service.claim(customerId, "stale-key", request);

        assertThat(recovered.status()).isEqualTo(OrderPlacementClaim.Status.ACTIVE);
        assertThat(recovered.reservedOrderId()).isEqualTo(first.reservedOrderId());
        assertThat(recovered.claimToken()).isNotEqualTo(first.claimToken());
    }

    @Test
    void sameKeyWithDifferentFingerprintIsRejected() {
        UUID customerId = UUID.randomUUID();
        service.claim(customerId, "key-3", request(UUID.randomUUID(), 4L, PaymentMethod.COD));

        assertThatThrownBy(() -> service.claim(
                customerId,
                "key-3",
                request(UUID.randomUUID(), 4L, PaymentMethod.COD)))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_IDEMPOTENCY_CONFLICT);
    }

    @Test
    void sameOpaqueKeyIsScopedByCustomer() {
        CreateOrderRequest request = request(UUID.randomUUID(), 4L, PaymentMethod.COD);

        OrderPlacementClaim first = service.claim(UUID.randomUUID(), "shared-key", request);
        OrderPlacementClaim second = service.claim(UUID.randomUUID(), "shared-key", request);

        assertThat(first.status()).isEqualTo(OrderPlacementClaim.Status.ACTIVE);
        assertThat(second.status()).isEqualTo(OrderPlacementClaim.Status.ACTIVE);
        assertThat(second.reservedOrderId()).isNotEqualTo(first.reservedOrderId());
        assertThat(requests.count()).isEqualTo(2);
    }

    @Test
    void concurrentSameKeyCreatesOneClaimAndOneStableOrderId() throws Exception {
        UUID customerId = UUID.randomUUID();
        CreateOrderRequest request = request(UUID.randomUUID(), 4L, PaymentMethod.COD);
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<OrderPlacementClaim> operation = () -> {
                barrier.await();
                return service.claim(customerId, "concurrent-key", request);
            };
            List<Future<OrderPlacementClaim>> futures = executor.invokeAll(List.of(operation, operation));
            List<OrderPlacementClaim> claims = futures.stream().map(this::get).toList();

            assertThat(claims).extracting(OrderPlacementClaim::status)
                    .containsExactlyInAnyOrder(
                            OrderPlacementClaim.Status.ACTIVE,
                            OrderPlacementClaim.Status.IN_PROGRESS);
            assertThat(claims.get(0).reservedOrderId()).isEqualTo(claims.get(1).reservedOrderId());
            assertThat(requests.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void missingKeyIsRejectedBeforeAPlacementRecordIsCreated() {
        assertThatThrownBy(() -> service.claim(
                UUID.randomUUID(),
                " ",
                request(UUID.randomUUID(), 4L, PaymentMethod.COD)))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_IDEMPOTENCY_KEY_REQUIRED);
        assertThat(requests.count()).isZero();
    }

    @Test
    void oversizedKeyIsRejectedBeforeAPlacementRecordIsCreated() {
        String oversizedKey = "x".repeat(201);

        assertThatThrownBy(() -> service.claim(
                UUID.randomUUID(),
                oversizedKey,
                request(UUID.randomUUID(), 4L, PaymentMethod.COD)))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_IDEMPOTENCY_KEY_INVALID);
        assertThat(requests.count()).isZero();
    }

    private OrderPlacementClaim get(Future<OrderPlacementClaim> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError("Concurrent claim failed", exception);
        }
    }

    private CreateOrderRequest request(UUID branchId, long cartVersion, PaymentMethod method) {
        return new CreateOrderRequest(
                branchId,
                cartVersion,
                CheckoutDeliveryTargetRequest.savedAddress(UUID.randomUUID()),
                method
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestClockConfiguration {

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }
}
