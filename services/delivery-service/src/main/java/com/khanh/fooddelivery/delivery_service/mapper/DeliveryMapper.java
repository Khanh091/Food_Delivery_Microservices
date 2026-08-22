package com.khanh.fooddelivery.delivery_service.mapper;

import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryOfferResponse;
import com.khanh.fooddelivery.delivery_service.dto.response.CurrentDeliveryOfferResponse;
import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryResponse;
import com.khanh.fooddelivery.delivery_service.model.Delivery;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOffer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DeliveryMapper {

    DeliveryResponse toResponse(Delivery delivery);

    DeliveryOfferResponse toResponse(DeliveryOffer offer);

    @org.mapstruct.Mapping(target = "offerId", source = "offer.id")
    @org.mapstruct.Mapping(target = "deliveryId", source = "delivery.id")
    @org.mapstruct.Mapping(target = "offeredAt", source = "offer.offeredAt")
    @org.mapstruct.Mapping(target = "expiresAt", source = "offer.expiresAt")
    @org.mapstruct.Mapping(target = "deliveryStatus", source = "delivery.status")
    @org.mapstruct.Mapping(target = "restaurantName", source = "delivery.restaurantName")
    @org.mapstruct.Mapping(target = "branchName", source = "delivery.branchName")
    @org.mapstruct.Mapping(target = "pickupLatitude", source = "delivery.pickupLatitude")
    @org.mapstruct.Mapping(target = "pickupLongitude", source = "delivery.pickupLongitude")
    @org.mapstruct.Mapping(target = "pickupAddress", source = "delivery.pickupAddress")
    @org.mapstruct.Mapping(target = "customerAddressLabel", source = "delivery.customerAddressLabel")
    @org.mapstruct.Mapping(target = "customerAddress", source = "delivery.customerAddress")
    @org.mapstruct.Mapping(target = "customerLatitude", source = "delivery.customerLatitude")
    @org.mapstruct.Mapping(target = "customerLongitude", source = "delivery.customerLongitude")
    @org.mapstruct.Mapping(target = "paymentMethod", source = "delivery.paymentMethod")
    @org.mapstruct.Mapping(target = "requiredRestaurantAdvance", source = "delivery.requiredRestaurantAdvance")
    @org.mapstruct.Mapping(target = "customerCashToCollect", source = "delivery.customerCashToCollect")
    @org.mapstruct.Mapping(target = "driverGrossEarning", source = "delivery.driverGrossEarning")
    @org.mapstruct.Mapping(target = "restaurantCommissionAmount", source = "delivery.restaurantCommissionAmount")
    @org.mapstruct.Mapping(target = "driverCommissionAmount", source = "delivery.driverCommissionAmount")
    @org.mapstruct.Mapping(target = "driverNetEarning", source = "delivery.driverNetEarning")
    @org.mapstruct.Mapping(target = "restaurantNetAmount", source = "delivery.restaurantNetAmount")
    @org.mapstruct.Mapping(target = "platformRevenueAmount", source = "delivery.platformRevenueAmount")
    @org.mapstruct.Mapping(target = "restaurantAdvanceConfirmed", source = "delivery.restaurantAdvanceConfirmed")
    @org.mapstruct.Mapping(target = "customerCashCollected", source = "delivery.customerCashCollected")
    CurrentDeliveryOfferResponse toCurrentOffer(DeliveryOffer offer, Delivery delivery);
}
