package com.khanh.fooddelivery.payment_service.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.payment_service.dto.request.InternalCreatePaymentRequest;
import com.khanh.fooddelivery.payment_service.dto.response.PaymentResponse;
import com.khanh.fooddelivery.payment_service.entity.FinancialSnapshot;
import com.khanh.fooddelivery.payment_service.entity.Payment;
import com.khanh.fooddelivery.payment_service.exception.PaymentException;
import com.khanh.fooddelivery.payment_service.mapper.PaymentMapper;
import com.khanh.fooddelivery.payment_service.model.PaymentMethod;
import com.khanh.fooddelivery.payment_service.model.PaymentProvider;
import com.khanh.fooddelivery.payment_service.model.PaymentStatus;
import com.khanh.fooddelivery.payment_service.provider.PaymentProviderGateway;
import com.khanh.fooddelivery.payment_service.provider.PaymentProviderResolver;
import com.khanh.fooddelivery.payment_service.repository.FinancialSnapshotRepository;
import com.khanh.fooddelivery.payment_service.service.impl.PaymentServiceImpl;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTests {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID RESTAURANT_ID = UUID.randomUUID();

    @Mock private PaymentTransactionService transactions;
    @Mock private FinancialSnapshotRepository snapshots;
    @Mock private FeePolicyService feePolicies;
    @Mock private FinancialCalculator calculator;
    @Mock private PaymentMapper mapper;
    @Mock private PaymentProviderResolver providers;

    private PaymentServiceImpl service;
    private InternalCreatePaymentRequest request;
    private Payment existing;
    private FinancialSnapshot snapshot;

    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl(
                transactions,
                snapshots,
                feePolicies,
                calculator,
                mapper,
                providers,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
        );
        request = new InternalCreatePaymentRequest(
                ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, UUID.randomUUID(), PaymentMethod.COD,
                new BigDecimal("100.00"), new BigDecimal("20.00"), BigDecimal.ZERO,
                new BigDecimal("120.00"), "VND", "order:" + ORDER_ID + ":payment");
        existing = new Payment();
        existing.setId(UUID.randomUUID());
        existing.setOrderId(ORDER_ID);
        existing.setCustomerUserId(CUSTOMER_ID);
        existing.setRestaurantId(RESTAURANT_ID);
        existing.setMethod(PaymentMethod.COD);
        existing.setStatus(PaymentStatus.PENDING);
        existing.setAmount(new BigDecimal("120.00"));
        existing.setCurrency("VND");
        existing.setProvider(PaymentProvider.COD);
        snapshot = new FinancialSnapshot();
        snapshot.setRestaurantId(RESTAURANT_ID);
        snapshot.setFoodGrossAmount(new BigDecimal("100.00"));
        snapshot.setDeliveryGrossAmount(new BigDecimal("20.00"));
        snapshot.setCustomerPayableAmount(new BigDecimal("120.00"));
        snapshot.setCurrency("VND");
    }

    @Test
    void sameOrderAndPaymentIntentReuseExistingPayment() {
        when(transactions.findExisting(ORDER_ID, request.idempotencyKey())).thenReturn(Optional.of(existing));
        when(snapshots.findByOrderId(ORDER_ID)).thenReturn(Optional.of(snapshot));
        when(mapper.toResponse(eq(existing), any(FinancialSnapshot.class))).thenReturn(mock(PaymentResponse.class));

        service.create(request);

        verify(transactions, never()).createPending(any(), any());
        verify(providers, never()).resolve(any());
    }

    @Test
    void incompatibleRetryCannotReuseExistingPayment() {
        when(transactions.findExisting(ORDER_ID, request.idempotencyKey())).thenReturn(Optional.of(existing));
        InternalCreatePaymentRequest changed = new InternalCreatePaymentRequest(
                ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, request.branchId(), PaymentMethod.COD,
                request.foodGrossAmount(), request.deliveryGrossAmount(), request.discountAmount(),
                new BigDecimal("121.00"), "VND", request.idempotencyKey());

        assertThatThrownBy(() -> service.create(changed))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("incompatible");
        verify(providers, never()).resolve(any());
    }

    @Test
    void existingPaymentReturnedByCreatePendingIsValidatedBeforeReuse() {
        when(transactions.findExisting(ORDER_ID, request.idempotencyKey())).thenReturn(Optional.empty());
        when(feePolicies.currentPolicy(any())).thenReturn(null);
        when(calculator.calculate(eq(request), eq(null))).thenReturn(null);
        when(transactions.createPending(eq(request), eq(null))).thenReturn(existing);
        when(snapshots.findByOrderId(ORDER_ID)).thenReturn(Optional.of(snapshot));
        when(mapper.toResponse(eq(existing), any(FinancialSnapshot.class))).thenReturn(mock(PaymentResponse.class));

        service.create(request);

        verify(transactions).createPending(request, null);
    }
}
