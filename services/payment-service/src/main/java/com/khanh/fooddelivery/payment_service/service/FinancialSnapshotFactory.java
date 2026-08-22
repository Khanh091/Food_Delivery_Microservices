package com.khanh.fooddelivery.payment_service.service;

import com.khanh.fooddelivery.payment_service.dto.request.InternalCreatePaymentRequest;
import com.khanh.fooddelivery.payment_service.entity.FinancialSnapshot;
import com.khanh.fooddelivery.payment_service.model.FinancialSnapshotStatus;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class FinancialSnapshotFactory {
    public FinancialSnapshot create(InternalCreatePaymentRequest request, FinancialBreakdown breakdown) {
        FinancialSnapshot snapshot = new FinancialSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setOrderId(request.orderId());
        snapshot.setRestaurantId(request.restaurantId());
        snapshot.setFeePolicyId(breakdown.feePolicyId());
        snapshot.setFeePolicyVersion(breakdown.feePolicyVersion());
        snapshot.setFoodGrossAmount(breakdown.foodGrossAmount());
        snapshot.setDeliveryGrossAmount(breakdown.deliveryGrossAmount());
        snapshot.setRestaurantCommissionRate(breakdown.restaurantCommissionRate());
        snapshot.setRestaurantCommissionAmount(breakdown.restaurantCommissionAmount());
        snapshot.setRestaurantNetAmount(breakdown.restaurantNetAmount());
        snapshot.setDriverCommissionRate(breakdown.driverCommissionRate());
        snapshot.setDriverCommissionAmount(breakdown.driverCommissionAmount());
        snapshot.setDriverNetAmount(breakdown.driverNetAmount());
        snapshot.setPlatformRevenueAmount(breakdown.platformRevenueAmount());
        snapshot.setCustomerPayableAmount(breakdown.customerPayableAmount());
        snapshot.setPaymentProcessingFee(breakdown.paymentProcessingFee());
        snapshot.setCurrency(breakdown.currency());
        snapshot.setStatus(FinancialSnapshotStatus.OPEN);
        return snapshot;
    }
}
