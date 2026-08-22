package com.khanh.fooddelivery.payment_service.mapper;

import com.khanh.fooddelivery.payment_service.dto.response.PaymentResponse;
import com.khanh.fooddelivery.payment_service.entity.FinancialSnapshot;
import com.khanh.fooddelivery.payment_service.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(target = "id", source = "payment.id")
    @Mapping(target = "orderId", source = "payment.orderId")
    @Mapping(target = "method", source = "payment.method")
    @Mapping(target = "status", source = "payment.status")
    @Mapping(target = "amount", source = "payment.amount")
    @Mapping(target = "currency", source = "payment.currency")
    @Mapping(target = "provider", source = "payment.provider")
    @Mapping(target = "providerTransactionId", source = "payment.providerTransactionId")
    @Mapping(target = "providerReference", source = "payment.providerReference")
    @Mapping(target = "paidAt", source = "payment.paidAt")
    @Mapping(target = "collectedAt", source = "payment.collectedAt")
    @Mapping(target = "refundedAt", source = "payment.refundedAt")
    @Mapping(target = "feePolicyId", source = "snapshot.feePolicyId")
    @Mapping(target = "feePolicyVersion", source = "snapshot.feePolicyVersion")
    @Mapping(target = "restaurantCommissionAmount", source = "snapshot.restaurantCommissionAmount")
    @Mapping(target = "restaurantNetAmount", source = "snapshot.restaurantNetAmount")
    @Mapping(target = "driverCommissionAmount", source = "snapshot.driverCommissionAmount")
    @Mapping(target = "driverNetAmount", source = "snapshot.driverNetAmount")
    @Mapping(target = "platformRevenueAmount", source = "snapshot.platformRevenueAmount")
    PaymentResponse toResponse(Payment payment, FinancialSnapshot snapshot);
}
